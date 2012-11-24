package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.simpleframework.ado.db.IQueryEntitySet;
import net.simpleframework.ado.db.ITableEntityService;
import net.simpleframework.ado.db.common.SqlUtils;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.ado.IParamsValue;
import net.simpleframework.common.ado.query.IDataQuery;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.EProcessModelStatus;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.IProcessManager;
import net.simpleframework.workflow.engine.IProcessModelManager;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.InitiateItems;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.ProcessModelLobBean;
import net.simpleframework.workflow.engine.participant.IParticipantModel;
import net.simpleframework.workflow.schema.AbstractParticipantType;
import net.simpleframework.workflow.schema.AbstractParticipantType.Role;
import net.simpleframework.workflow.schema.AbstractParticipantType.User;
import net.simpleframework.workflow.schema.AbstractProcessStartupType;
import net.simpleframework.workflow.schema.AbstractProcessStartupType.Manual;
import net.simpleframework.workflow.schema.ProcessDocument;
import net.simpleframework.workflow.schema.ProcessNode;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ProcessModelManager extends AbstractWorkflowManager<ProcessModelBean> implements
		IProcessModelManager {

	@Override
	public ProcessDocument getProcessDocument(final ProcessModelBean processModel) {
		ProcessDocument doc = (ProcessDocument) processModel.getAttr("processDocument");
		if (doc == null) {
			final ProcessModelLobBean lob = getModuleContext().getEntityService(
					ProcessModelLobBean.class).getBean(processModel.getId(), ProcessModelLobBean.class);
			if (lob != null) {
				processModel.setAttr("processDocument",
						doc = new ProcessDocument(lob.getProcessSchema()));
			}
		}
		return doc;
	}

	@Override
	public ProcessModelBean addModel(final ID userId, ProcessDocument document) {
		final ProcessModelBean bean = createBean();
		if (document == null) {
			document = new ProcessDocument();
		}
		final ProcessNode processNode = document.getProcessNode();
		if (userId != null) {
			bean.setUserId(userId);
			if (!StringUtils.hasText(processNode.getAuthor())) {
				processNode.setAuthor(getModuleContext().getParticipantMgr().getUser(userId).getText());
			}
		}
		bean.setModelName(processNode.getName());
		bean.setModelText(processNode.getText());
		bean.setCreateDate(new Date());

		insert(bean);

		final String schema = document.toString();
		final ProcessModelLobBean lob = new ProcessModelLobBean();
		lob.setId(bean.getId());
		lob.setProcessSchema(schema.toCharArray());
		getModuleContext().getEntityService(ProcessModelLobBean.class).insert(lob);

		return bean;
	}

	@Override
	public void updateModel(final ProcessModelBean processModel, final ID userId, final char[] model) {
		try {
			final ProcessDocument document = new ProcessDocument(model);
			final ProcessNode processNode = document.getProcessNode();
			processModel.setModelName(processNode.getName());
			processModel.setModelText(processNode.getText());
			if (userId != null) {
				processModel.setLastUserId(userId);
			}
			processModel.setLastUpdate(new Date());
			update(processModel);

			final ProcessModelLobBean lob = getEntityService(ProcessModelLobBean.class).getBean(
					processModel.getId(), ProcessModelLobBean.class);
			lob.setProcessSchema(model);
			getEntityService(ProcessModelLobBean.class).update(lob);
		} finally {
			processModel.removeAttr("processDocument");
		}
	}

	@Override
	public IDataQuery<ProcessModelBean> getModelList(final EProcessModelStatus... status) {
		final StringBuilder sql = new StringBuilder();
		final ArrayList<Object> params = new ArrayList<Object>();
		sql.append("1=1");
		if (status != null && status.length > 0) {
			sql.append(" and (");
			int i = 0;
			for (final EProcessModelStatus s : status) {
				if (i++ > 0) {
					sql.append(" or ");
				}
				sql.append("status=?");
				params.add(s);
			}
			sql.append(")");
		}
		sql.append(" order by createDate desc");
		return query(sql.toString(), params.toArray());
	}

	@Override
	public ProcessModelBean getProcessModelByName(final String name) {
		return getBean("modelName=?", name);
	}

	@Override
	public ProcessModelBean getProcessModel(final String model) {
		ProcessModelBean processModel = getProcessModelByName(model);
		if (processModel == null) {
			processModel = getBean(model);
		}
		if (processModel == null) {
			throw WorkflowException.of($m("ProcessModelManager.1", model));
		}
		return processModel;
	}

	private final Map<ID, InitiateItems> itemsCache = new HashMap<ID, InitiateItems>();

	@Override
	public InitiateItems getInitiateItems(final ID userId) {
		if (userId == null) {
			return InitiateItems.NULL_ITEMS;
		}
		InitiateItems items = itemsCache.get(userId);
		if (items != null) {
			items = items.clone();
			for (final InitiateItem item : items) {
				if (item.model() == null || item.getInitiateRoles().size() == 0) {
					items.remove(item.getModelId());
				}
			}
			return items;
		}
		final IParticipantModel participantMgr = getModuleContext().getParticipantMgr();
		items = new InitiateItems();
		final IDataQuery<ProcessModelBean> query = getModelList(EProcessModelStatus.deploy);
		ProcessModelBean processModel;
		while ((processModel = query.next()) != null) {
			final AbstractProcessStartupType startupType = getProcessDocument(processModel)
					.getProcessNode().getStartupType();
			if (startupType instanceof Manual) {
				final KVMap variables = new KVMap().add("model", processModel);
				final AbstractParticipantType pt = ((Manual) startupType).getParticipantType();
				final String participant = pt.getParticipant();
				if (pt instanceof User) {
					final ID userId2 = participantMgr.getUser(participant).getId();
					if (userId.equals(userId2)) {
						final Collection<ID> roleIds = participantMgr.roles(userId, variables);
						if (roleIds.size() > 0) {
							items.add(new InitiateItem(processModel, userId, roleIds));
						}
					}
				} else if (pt instanceof Role) {
					final ID roleId = participantMgr.getRole(participant).getId();
					if (participantMgr.isMember(userId, roleId, variables)) {
						items.add(new InitiateItem(processModel, userId, roleId));
					}
				}
			} else {
			}
		}
		itemsCache.put(userId, items);
		return items;
	}

	@Override
	public boolean isStartProcess(final ID userId, final Object model) {
		return getInitiateItems(userId).get(model) != null;
	}

	@Override
	public void setStatus(final ProcessModelBean processModel, final EProcessModelStatus status) {
		processModel.setStatus(status);
		update(new String[] { "status" }, processModel);
	}

	@Override
	public KVMap createVariables(final ProcessModelBean model) {
		final KVMap variables = super.createVariables(model);
		variables.add("model", model);
		return variables;
	}

	{
		addListener(new TableEntityAdapterEx() {

			@Override
			public void afterInsert(final ITableEntityService manager, final Object[] objects) {
				itemsCache.clear();
			}

			@Override
			public void afterUpdate(final ITableEntityService manager, final Object[] objects) {
				itemsCache.clear();
			}

			@Override
			public void beforeDelete(final ITableEntityService manager, final IParamsValue paramsValue) {
				final IQueryEntitySet<Map<String, Object>> qs = manager.query(
						new String[] { "status" }, paramsValue);
				Map<String, Object> data;
				while ((data = qs.next()) != null) {
					final EProcessModelStatus status = Convert.toEnum(EProcessModelStatus.class,
							data.get("status"));
					if (status == EProcessModelStatus.deploy) {
						throw WorkflowException.of($m("ProcessModelManager.0"));
					}
				}
			}

			@Override
			public void afterDelete(final ITableEntityService manager, final IParamsValue paramsValue) {
				itemsCache.clear();

				// 删除流程实例
				final IProcessManager processMgr = processMgr();
				final Object[] modelIds = paramsValue.getValues();
				final Object[] processIds = processMgr.list("id",
						SqlUtils.getIdsSQLParam("modelId", modelIds.length), modelIds).toArray();
				if (processIds.length > 0) {
					processMgr.delete(processIds);
				}

				// 删除流程变量，静态
				VariableManager.get().deleteVariables(EVariableSource.model, modelIds);
			}
		});
	}
}
