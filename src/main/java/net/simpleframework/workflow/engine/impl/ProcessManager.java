package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import net.simpleframework.ado.db.ITableEntityService;
import net.simpleframework.ado.db.common.SqlUtils;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.ado.IParamsValue;
import net.simpleframework.common.ado.query.DataQueryUtils;
import net.simpleframework.common.ado.query.IDataQuery;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.task.ExecutorRunnable;
import net.simpleframework.common.task.TaskExecutor;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EActivityAbortPolicy;
import net.simpleframework.workflow.engine.EProcessAbortPolicy;
import net.simpleframework.workflow.engine.EProcessModelStatus;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IActivityManager;
import net.simpleframework.workflow.engine.IProcessManager;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.ProcessLobBean;
import net.simpleframework.workflow.engine.ProcessModelBean;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.event.IProcessListener;
import net.simpleframework.workflow.engine.event.IWorkflowListener;
import net.simpleframework.workflow.engine.remote.IProcessRemote;
import net.simpleframework.workflow.schema.ProcessDocument;
import net.simpleframework.workflow.schema.ProcessNode;
import net.simpleframework.workflow.schema.StartNode;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.VariableNode;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ProcessManager extends AbstractWorkflowManager<ProcessBean> implements IProcessManager {

	@Override
	public ProcessModelBean getProcessModel(final ProcessBean process) {
		return modelMgr().getBean(process.getModelId());
	}

	/**
	 * 获取流程的定义
	 * 
	 * @param process
	 * @return
	 */
	ProcessNode processNode(final ProcessBean process) {
		return modelMgr().getProcessDocument(getProcessModel(process)).getProcessNode();
	}

	@Override
	public ProcessBean startProcess(final InitiateItem initiateItem, final String topic) {
		final ID roleId = initiateItem.getSelectedRoleId();
		if (roleId == null) {
			throw WorkflowException.of($m("ProcessManager.2"));
		}
		final ProcessModelBean processModel = initiateItem.model();
		if (processModel == null) {
			throw WorkflowException.of($m("ProcessManager.3"));
		}

		final ProcessBean process = startProcess(processModel, initiateItem.getUserId(), roleId,
				initiateItem.getVariables(), null, topic);
		// 事件
		for (final IWorkflowListener listener : getEventListeners(process)) {
			((IProcessListener) listener).onProcessCreated(initiateItem, process);
		}
		createStartNode(process, initiateItem.getTransitions());
		return process;
	}

	@Override
	public ProcessBean startProcess(final InitiateItem initiateItem) {
		return startProcess(initiateItem, null);
	}

	@Override
	public ProcessBean startProcess(final ProcessModelBean processModel, final KVMap variables,
			final Properties properties, final String topic) {
		final ProcessBean process = startProcess(processModel, null, null, variables, properties,
				topic);
		for (final IWorkflowListener listener : getEventListeners(process)) {
			((IProcessListener) listener).onProcessCreated(null, process);
		}
		createStartNode(process, null);
		return process;
	}

	private void createStartNode(final ProcessBean process,
			final Collection<TransitionNode> transitions) {
		// 创建开始任务
		final StartNode startNode = processNode(process).startNode();
		final ActivityBean sActivity = activityMgr().createActivity(process, startNode, null);
		activityMgr().insert(sActivity);
		if (transitions == null) {
			new ActivityComplete(sActivity).complete();
		} else {
			new ActivityComplete(sActivity, transitions).complete();
		}
	}

	private ProcessBean startProcess(final ProcessModelBean processModel, final ID userId,
			final ID roleId, final KVMap variables, final Properties properties, final String topic) {
		if (processModel.getStatus() != EProcessModelStatus.deploy) {
			throw WorkflowException.of($m("ProcessManager.1"));
		}

		final ProcessBean process = createBean();
		process.setModelId(processModel.getId());
		process.setUserId(userId);
		process.setRoleId(roleId);
		process.setCreateDate(new Date());
		process.setTitle(topic);
		if (properties != null) {
			process.getProperties().putAll(properties);
		}
		insert(process);

		if (variables != null && variables.size() > 0) {
			// 绑定流程变量
			final Collection<String> names = getVariableNames(process);
			for (final Map.Entry<String, Object> e : variables.entrySet()) {
				final String key = e.getKey();
				if (names.contains(key)) {
					setVariable(process, key, e.getValue());
				}
			}
		}

		final ProcessDocument doc = modelMgr().getProcessDocument(processModel);
		if (!doc.getProcessNode().isInstanceShared()) {
			final ProcessLobBean lob = new ProcessLobBean();
			lob.setId(process.getId());
			lob.setProcessModel(doc.toString().toCharArray());
			getModuleContext().getEntityService(ProcessLobBean.class).insert(lob);
		}
		return process;
	}

	@Override
	public void backToRemote(final ProcessBean process) {
		taskExecutor.addScheduledTask(WorkflowSettings.get().getSubTaskPeriod(),
				new ExecutorRunnable() {
					@Override
					protected void task() throws Exception {
						final Properties properties = process.getProperties();
						final KVMap data = new KVMap(); // 提交的参数
						data.add(IProcessRemote.SUB_ACTIVITYID,
								properties.getProperty(IProcessRemote.SUB_ACTIVITYID));
						final String[] mappings = StringUtils.split(properties
								.getProperty(IProcessRemote.VAR_MAPPINGS));
						if (mappings != null) {
							final IProcessManager processMgr = getModuleContext().getProcessMgr();
							for (final String mapping : mappings) {
								data.add(mapping, processMgr.getVariable(process, mapping));
							}
						}

						final Map<String, Object> r = getModuleContext().getRemoteMgr().call(
								properties.getProperty(IProcessRemote.SERVERURL), "subComplete", data);
						final Boolean success = (Boolean) r.get("success");
						if (success != null && success.booleanValue()) {
							taskExecutor.removeScheduledTask(this);
						}
					}
				});
	}

	@Override
	public IDataQuery<ProcessBean> getProcessList(final ProcessModelBean processModel,
			final EProcessStatus... status) {
		if (processModel == null) {
			return DataQueryUtils.nullQuery();
		}
		final StringBuilder sql = new StringBuilder();
		final ArrayList<Object> params = new ArrayList<Object>();
		sql.append("modelId=?");
		params.add(processModel.getId());
		if (status != null && status.length > 0) {
			sql.append(" and (");
			int i = 0;
			for (final EProcessStatus s : status) {
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
	public void suspend(final ProcessBean process, final boolean resume) {
		if (resume) {
			assertStatus(process, EProcessStatus.suspended);
			process.setStatus(EProcessStatus.running);
		} else {
			assertStatus(process, EProcessStatus.running);
			process.setStatus(EProcessStatus.suspended);
		}
		update(new String[] { "status" }, process);

		for (final IWorkflowListener listener : getEventListeners(process)) {
			((IProcessListener) listener).onSuspend(process);
		}
	}

	@Override
	public void abort(final ProcessBean process, final EProcessAbortPolicy policy) {
		process.setStatus(EProcessStatus.abort);
		process.setCompleteDate(new Date());
		update(new String[] { "status", "completeDate" }, process);

		if (policy == EProcessAbortPolicy.allActivities) {
			final IDataQuery<ActivityBean> qs = activityMgr().getActivities(process);
			ActivityBean activity;
			final ActivityManager activityMgr = activityMgr();
			while ((activity = qs.next()) != null) {
				activityMgr._abort(activity, EActivityAbortPolicy.normal, false);
			}
		}

		for (final IWorkflowListener listener : getEventListeners(process)) {
			((IProcessListener) listener).onAbort(process, policy);
		}
	}

	@Override
	public KVMap createVariables(final ProcessBean process) {
		final KVMap variables = modelMgr().createVariables(getProcessModel(process));
		variables.add("process", process);
		for (final String variable : getVariableNames(process)) {
			variables.add(variable, getVariable(process, variable));
		}
		return variables;
	}

	@Override
	public Object getVariable(final ProcessBean process, final String name) {
		final VariableNode variableNode = processNode(process).getVariableNodeByName(name);
		return VariableManager.get().getVariableValue(process, variableNode);
	}

	@Override
	public void setVariable(final ProcessBean process, final String name, final Object value) {
		setVariable(process, new String[] { name }, new Object[] { value });
	}

	@Override
	public void setVariable(final ProcessBean process, final String[] names, final Object[] values) {
		VariableManager.get().setVariableValue(process, names, values);
	}

	@Override
	public Collection<String> getVariableNames(final ProcessBean process) {
		return processNode(process).variables().keySet();
	}

	@Override
	public WorkitemBean getFirstWorkitem(final ProcessBean process) {
		final IActivityManager activityMgr = activityMgr();
		final ActivityBean startActivity = activityMgr.getStartActivity(process);
		final IDataQuery<ActivityBean> qs = activityMgr.getNextActivities(startActivity);
		ActivityBean activity;
		while ((activity = qs.next()) != null) {
			final WorkitemBean workitem = workitemMgr().getWorkitemList(activity,
					EWorkitemStatus.running).next();
			if (workitem != null) {
				return workitem;
			}
		}
		throw WorkflowException.of($m("ProcessManager.0"));
	}

	@Override
	public boolean isFinalStatus(final ProcessBean process) {
		final EProcessStatus status = process.getStatus();
		return status == EProcessStatus.complete || status == EProcessStatus.abort;
	}

	@Override
	public void setProcessTitle(final ProcessBean process, final String title) {
		process.setTitle(title);
		update(new String[] { "title" }, process);
	}

	private final TaskExecutor taskExecutor = new TaskExecutor();

	{
		addListener(new TableEntityAdapterEx() {

			@Override
			public void afterDelete(final ITableEntityService manager, final IParamsValue paramsValue) {
				// 删除任务环节
				final IActivityManager activityMgr = activityMgr();
				final Object[] processIds = paramsValue.getValues();
				final Object[] activityIds = activityMgr.list("id",
						SqlUtils.getIdsSQLParam("processId", processIds.length), processIds).toArray();
				if (activityIds.length > 0) {
					activityMgr.delete(activityIds);
				}

				// 删除流程变量
				VariableManager.get().deleteVariables(EVariableSource.process, processIds);
			}
		});
	}
}
