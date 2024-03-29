package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.simpleframework.ado.db.ITableEntityService;
import net.simpleframework.ado.db.common.SqlUtils;
import net.simpleframework.common.ClassUtils;
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
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EVariableSource;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IActivityManager;
import net.simpleframework.workflow.engine.IMappingVal;
import net.simpleframework.workflow.engine.IProcessManager;
import net.simpleframework.workflow.engine.IWorkitemManager;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.event.IActivityListener;
import net.simpleframework.workflow.engine.event.IWorkflowListener;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.engine.participant.ParticipantUtils;
import net.simpleframework.workflow.engine.remote.IProcessRemote;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.EVariableMode;
import net.simpleframework.workflow.schema.EndNode;
import net.simpleframework.workflow.schema.ProcessNode;
import net.simpleframework.workflow.schema.SubNode;
import net.simpleframework.workflow.schema.SubNode.VariableMapping;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.UserNode;
import net.simpleframework.workflow.schema.VariableNode;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ActivityManager extends AbstractWorkflowManager<ActivityBean> implements
		IActivityManager {

	@Override
	public void complete(final ActivityComplete activityComplete) {
		final ActivityBean activity = activityComplete.getActivity();
		if (isFinalStatus(activity)) {
			throw WorkflowStatusException.of($m("ActivityManager.2"));
		}

		final ProcessBean process = getProcessBean(activity);
		ActivityBean endActivity = null;
		// 如果流程处在最终状态，则不创建后续环节
		if (!processMgr().isFinalStatus(process)) {
			for (final TransitionNode transition : activityComplete.getTransitions()) {
				final AbstractTaskNode to = transition.to();
				if (to instanceof UserNode) {
					doUserNode(activity, (UserNode) to, activityComplete.getParticipants(transition));
				} else if (to instanceof SubNode) {
					doSubNode(activity, (SubNode) to);
				} else if (to instanceof EndNode) {
					endActivity = createActivity(process, to, activity);
					endActivity.setStatus(EActivityStatus.complete);
					endActivity.setCompleteDate(new Date());
					insert(endActivity);
				}
			}
		}

		activity.setStatus(EActivityStatus.complete);
		activity.setCompleteDate(new Date());
		update(new String[] { "completeDate", "status" }, activity);

		final AbstractTaskNode tasknode = taskNode(activity);
		if (tasknode instanceof UserNode) {
			// 放弃未完成的工作项
			final IWorkitemManager workitemMgr = workitemMgr();
			final IDataQuery<WorkitemBean> qs = workitemMgr.getWorkitemList(activity,
					EWorkitemStatus.running, EWorkitemStatus.suspended);
			WorkitemBean workitem;
			while ((workitem = qs.next()) != null) {
				workitem.setStatus(EWorkitemStatus.abort);
				workitem.setCompleteDate(new Date());
				workitemMgr.update(new String[] { "completeDate", "status" }, workitem);
			}

			if (!ParticipantUtils.isInstanceShared(tasknode)
					&& !ParticipantUtils.isSequential(tasknode)) {
				// 多实例，响应数
				final ArrayList<ActivityBean> al = new ArrayList<ActivityBean>();
				final IDataQuery<ActivityBean> qs2 = getNextActivities(getPreActivity(activity));
				ActivityBean activity2;
				int complete = 0;
				while ((activity2 = qs2.next()) != null) {
					if (activity2.getTasknodeId().equals(tasknode.getId())) {
						al.add(activity2);
						if (activity2.getStatus() == EActivityStatus.complete) {
							complete++;
						}
					}
				}
				if (complete >= ParticipantUtils.getResponseValue(tasknode, al.size())) {
					for (int i = 0; i < al.size(); i++) {
						_abort(al.get(i), EActivityAbortPolicy.normal, false);
					}
				}
			}
		}

		if (endActivity != null) {
			process.setStatus(EProcessStatus.complete);
			process.setCompleteDate(endActivity.getCompleteDate());
			processMgr().update(new String[] { "completeDate", "status" }, process);

			backToProcess(process);
		}

		// 事件
		for (final IWorkflowListener listener : getEventListeners(activity)) {
			((IActivityListener) listener).onCompleted(activityComplete);
		}
	}

	private void doUserNode(final ActivityBean activity, final UserNode to,
			final Collection<Participant> _participants) {
		ActivityBean nActivity = null;
		final ArrayList<Participant> participants = new ArrayList<Participant>();
		Iterator<Participant> it = null;
		if (_participants != null && _participants.size() > 0) {
			if (ParticipantUtils.isSequential(to)) {
				it = _participants.iterator();
				participants.add(it.next());
			} else {
				participants.addAll(_participants);
			}
		}

		final WorkitemManager workitemMgr = workitemMgr();
		final boolean instanceShared = ParticipantUtils.isInstanceShared(to);
		final ProcessBean process = getProcessBean(activity);
		for (final Participant participant : participants) {
			if (!instanceShared || nActivity == null) {
				nActivity = createActivity(process, to, activity);
				PropSequential.set(nActivity, it);
				insert(nActivity);
			}
			workitemMgr.insert(workitemMgr.createWorkitem(nActivity, participant));
		}
	}

	private void backToProcess(final ProcessBean sProcess) {
		final Properties properties = sProcess.getProperties();
		final String serverUrl = properties.getProperty(IProcessRemote.SERVERURL);
		if (StringUtils.hasText(serverUrl)) {
			processMgr().backToRemote(sProcess);
		} else {
			final ActivityBean nActivity = getBean(properties
					.getProperty(IProcessRemote.SUB_ACTIVITYID));
			final IProcessManager processMgr = processMgr();
			subComplete(nActivity, new IMappingVal() {
				@Override
				public Object val(final String mapping) {
					return processMgr.getVariable(sProcess, mapping);
				}
			});
		}
	}

	@Override
	public void subComplete(final ActivityBean activity, final IMappingVal mappingVal) {
		if (activity == null) {
			return;
		}
		final SubNode sub = (SubNode) taskNode(activity);
		if (sub.isSync()) {
			// 设置返回的变量，仅在同步方式
			final ProcessManager processMgr = (ProcessManager) getModuleContext().getProcessMgr();
			final ProcessBean mProcess = getProcessBean(activity);
			final ProcessNode processNode = processMgr.processNode(mProcess);
			for (final VariableMapping vm : sub.getMappingSet()) {
				final VariableNode vNode = processNode.getVariableNodeByName(vm.variable);
				if (vNode == null) {
					continue;
				}
				if (vNode.getMode() == EVariableMode.inout) {
					processMgr.setVariable(mProcess, vm.variable, mappingVal.val(vm.mapping));
				}
			}
		}
		new ActivityComplete(activity).complete();
	}

	private void doSubNode(final ActivityBean preActivity, final SubNode to) {
		final ActivityBean nActivity = createActivity(to, preActivity);
		insert(nActivity);

		final String url = to.getUrl();
		// 如果是远程，则通过循环任务方式，如果本地，则进行强验证，即抛出异常
		if (!StringUtils.hasText(url)) {
			final IProcessManager processMgr = processMgr();
			final ProcessBean process = getProcessBean(preActivity);
			final KVMap variables = new KVMap();
			for (final VariableMapping vMapping : to.getMappingSet()) {
				final Object value = processMgr.getVariable(process, vMapping.variable);
				variables.add(vMapping.mapping, value);
			}
			final boolean sync = to.isSync();
			Properties properties = null;
			if (sync) {
				properties = new Properties();
				properties
						.setProperty(IProcessRemote.SUB_ACTIVITYID, String.valueOf(nActivity.getId()));
			}
			processMgr.startProcess(modelMgr().getProcessModel(to.getModel()), variables, properties,
					null);
			if (sync) {
				nActivity.setStatus(EActivityStatus.waiting);
				update(new String[] { "status" }, nActivity);
			} else {
				new ActivityComplete(nActivity).complete();
			}
		} else {
			doRemoteSubTask(nActivity);
		}
	}

	@Override
	public void doRemoteSubTask(final ActivityBean activity) {
		final ID activityId = activity.getId();
		taskExecutor.addScheduledTask(WorkflowSettings.get().getSubTaskPeriod(),
				new ExecutorRunnable() {
					@Override
					protected void task() throws Exception {
						final ActivityBean nActivity = getBean(activityId);
						if (nActivity == null) {
							taskExecutor.removeScheduledTask(this);
							return;
						}

						final ProcessBean mProcess = getProcessBean(nActivity);
						final IProcessManager processMgr = getModuleContext().getProcessMgr();
						final SubNode sub = (SubNode) taskNode(nActivity);
						final KVMap data = new KVMap(); // 提交的参数

						final EActivityStatus status = nActivity.getStatus();
						if (status == EActivityStatus.running) {
							// 模型名称、主流程的地址及实例id
							data.add(IProcessRemote.SERVERURL, WorkflowSettings.get().getServerUrl());
							data.add(IProcessRemote.SUB_ACTIVITYID, nActivity.getId());
							data.add(IProcessRemote.MODEL, sub.getModel());
							int i = 0;
							final StringBuilder mappings = new StringBuilder();
							for (final VariableMapping vMapping : sub.getMappingSet()) {
								if (i++ > 0) {
									mappings.append(";");
								}
								data.add(vMapping.mapping,
										processMgr.getVariable(mProcess, vMapping.variable));
								mappings.append(vMapping.mapping);
							}
							if (mappings.length() > 0) {
								data.add(IProcessRemote.VAR_MAPPINGS, mappings.toString());
							}

							// 创建远程子流程实例
							final Map<String, Object> r = getModuleContext().getRemoteMgr().call(
									sub.getUrl(), "startProcess", data);
							final Object processId = r.get(IProcessRemote.SUB_PROCESSID);
							if (processId != null) {
								if (sub.isSync()) {
									nActivity.setStatus(EActivityStatus.waiting);
									// 保存子流程id
									nActivity.getProperties().setProperty(IProcessRemote.SUB_PROCESSID,
											String.valueOf(processId));
									getEntityService(ActivityBean.class).update(
											new String[] { "status", "properties" }, nActivity);
								} else {
									new ActivityComplete(nActivity).complete();
								}
								taskExecutor.removeScheduledTask(this);
							}
						} else if (status == EActivityStatus.waiting) {
							// 如果发现环节处在等待状态，则发送一个远程检测请求来确认子流程是否完成
							final Properties properties = nActivity.getProperties();
							data.add(IProcessRemote.SUB_PROCESSID,
									properties.get(IProcessRemote.SUB_PROCESSID));
							try {
								final Map<String, Object> r = getModuleContext().getRemoteMgr().call(
										sub.getUrl(), "checkProcess", data);
								final Boolean success = (Boolean) r.get("success");
								if (success != null && success.booleanValue()) {
									taskExecutor.removeScheduledTask(this);
								}
							} catch (final IOException e) {
								// 忽略。启动时调用，不抛出异常
								log.warn(e);
							}
						}
					}
				});
	}

	void _abort(final ActivityBean activity, final EActivityAbortPolicy policy,
			final boolean throwThrowable) {
		if (isFinalStatus(activity)) {
			if (throwThrowable) {
				throw WorkflowStatusException.of($m("ActivityManager.3", activity.getStatus()));
			} else {
				return;
			}
		}

		final IWorkitemManager workitemMgr = workitemMgr();
		final IDataQuery<WorkitemBean> qs = workitemMgr.getWorkitemList(activity);
		WorkitemBean workitem;
		while ((workitem = qs.next()) != null) {
			if (!workitemMgr.isFinalStatus(workitem)) {
				workitem.setStatus(EWorkitemStatus.abort);
				workitem.setCompleteDate(new Date());
				workitemMgr.update(new String[] { "status", "completeDate" }, workitem);
			}
		}

		activity.setStatus(EActivityStatus.abort);
		activity.setCompleteDate(new Date());
		update(new String[] { "status", "completeDate" }, activity);

		if (policy == EActivityAbortPolicy.nextActivities) {
			final IDataQuery<ActivityBean> qs2 = getNextActivities(activity);
			ActivityBean nextActivity;
			while ((nextActivity = qs2.next()) != null) {
				_abort(nextActivity, EActivityAbortPolicy.nextActivities, false);
			}
		}

		// 触发事件
		for (final IWorkflowListener listener : getEventListeners(activity)) {
			((IActivityListener) listener).onAbort(activity, policy);
		}
	}

	@Override
	public void abort(final ActivityBean activity, final EActivityAbortPolicy policy) {
		_abort(activity, policy, true);
	}

	@Override
	public void suspend(final ActivityBean activity, final boolean resume) {
		if (resume) {
			assertStatus(activity, EActivityStatus.suspended);
			activity.setStatus(EActivityStatus.running);
		} else {
			assertStatus(activity, EActivityStatus.running);
			activity.setStatus(EActivityStatus.suspended);
		}
		update(new String[] { "status" }, activity);

		for (final IWorkflowListener listener : getEventListeners(activity)) {
			((IActivityListener) listener).onSuspend(activity);
		}
	}

	@Override
	public IDataQuery<ActivityBean> getActivities(final ProcessBean processBean,
			final EActivityStatus... status) {
		if (processBean == null) {
			return DataQueryUtils.nullQuery();
		}
		final StringBuilder sql = new StringBuilder();
		final ArrayList<Object> params = new ArrayList<Object>();
		sql.append("processId=?");
		params.add(processBean.getId());
		if (status != null && status.length > 0) {
			sql.append(" and (");
			int i = 0;
			for (final EActivityStatus s : status) {
				if (i++ > 0) {
					sql.append(" or ");
				}
				sql.append("status=?");
				params.add(s);
			}
			sql.append(")");
		}
		sql.append(" order by createDate asc");
		return query(sql.toString(), params.toArray());
	}

	@Override
	public void jump(final ActivityBean activity, final String tasknode) {
		assertStatus(activity, EActivityStatus.running);

		// final ProcessNode processNode = (ProcessNode)
		// activity.taskNode().parent();
		//
		// processNode.getNodeByName(tasknodeName);
	}

	@Override
	public void fallback(final ActivityBean activity, final String tasknode) {
		assertStatus(activity, EActivityStatus.running);
		final WorkitemManager workitemMgr = workitemMgr();
		if (workitemMgr.getWorkitemList(activity, EWorkitemStatus.complete).getCount() > 0) {
			throw WorkflowException.of($m("ActivityManager.0"));
		}
		final ActivityBean preActivity = getPreActivity(activity, tasknode);
		AbstractTaskNode to;
		if (preActivity == null || !((to = taskNode(preActivity)) instanceof UserNode)) {
			throw WorkflowException.of($m("ActivityManager.1"));
		}

		final IDataQuery<WorkitemBean> qs = workitemMgr.getWorkitemList(preActivity,
				EWorkitemStatus.complete);
		final ActivityBean nActivity = createActivity(to, activity);
		if (ParticipantUtils.isSequential(to)) {
			final List<WorkitemBean> list = DataQueryUtils.toList(qs);
			Collections.sort(list, new Comparator<WorkitemBean>() {
				@Override
				public int compare(final WorkitemBean item1, final WorkitemBean item2) {
					return item1.getCreateDate().compareTo(item2.getCreateDate());
				}
			});
			final ArrayList<Participant> al = new ArrayList<Participant>();
			for (final WorkitemBean item : list) {
				al.add(new Participant(item.getUserId(), item.getRoleId()));
			}
			final Iterator<Participant> it = al.iterator();
			final Participant p = it.next();
			PropSequential.set(nActivity, it);
			insert(nActivity);
			workitemMgr.insert(workitemMgr.createWorkitem(nActivity, p));
		} else {
			insert(nActivity);
			WorkitemBean workitem;
			while ((workitem = qs.next()) != null) {
				final Participant p = new Participant(workitem.getUserId(), workitem.getRoleId());
				workitemMgr.insert(workitemMgr.createWorkitem(nActivity, p));
			}
		}
		_abort(activity, EActivityAbortPolicy.normal, true);

		for (final IWorkflowListener listener : getEventListeners(activity)) {
			((IActivityListener) listener).onFallback(nActivity, tasknode);
		}
	}

	@Override
	public void fallback(final ActivityBean activity) {
		fallback(activity, null);
	}

	@Override
	public ActivityBean getStartActivity(final ProcessBean processBean) {
		return getBean("processId=? and previousId is null", processBean.getId());
	}

	@Override
	public IDataQuery<ActivityBean> getNextActivities(final ActivityBean preActivity) {
		if (preActivity == null) {
			return DataQueryUtils.nullQuery();
		}
		return query("previousId=?", preActivity.getId());
	}

	@Override
	public ActivityBean getPreActivity(final ActivityBean activity) {
		return getBean(activity.getPreviousId());
	}

	@Override
	public ActivityBean getPreActivity(final ActivityBean activity, final String tasknode) {
		ActivityBean preActivity = getPreActivity(activity);
		while (preActivity != null && tasknode != null) {
			if (tasknode.equals(preActivity.getTasknodeId())
					|| tasknode.equals(taskNode(preActivity).getName())) {
				break;
			}
			preActivity = getPreActivity(preActivity);
		}
		return preActivity;
	}

	@Override
	public ProcessBean getProcessBean(final ActivityBean activity) {
		return processMgr().getBean(activity.getProcessId());
	}

	@Override
	public AbstractTaskNode taskNode(final ActivityBean activity) {
		final AbstractTaskNode taskNode = (AbstractTaskNode) processMgr().processNode(
				getProcessBean(activity)).getNodeById(activity.getTasknodeId());
		assert taskNode != null;
		return taskNode;
	}

	@Override
	public KVMap createVariables(final ActivityBean activity) {
		final KVMap variables = processMgr().createVariables(getProcessBean(activity));
		variables.add("activity", activity);
		for (final String variable : getVariableNames(activity)) {
			variables.add(variable, getVariable(activity, variable));
		}
		return variables;
	}

	@Override
	public Object getVariable(final ActivityBean activity, final String name) {
		final VariableNode variableNode = taskNode(activity).getVariableNodeByName(name);
		return VariableManager.get().getVariableValue(activity, variableNode);
	}

	@Override
	public void setVariable(final ActivityBean activity, final String name, final Object value) {
		setVariable(activity, new String[] { name }, new Object[] { value });
	}

	@Override
	public void setVariable(final ActivityBean activity, final String[] names, final Object[] values) {
		VariableManager.get().setVariableValue(activity, names, values);
	}

	@Override
	public Collection<String> getVariableNames(final ActivityBean activity) {
		return taskNode(activity).variables().keySet();
	}

	private final KVMap formInstancCache = new KVMap();

	@Override
	public Object getWorkflowForm(final ActivityBean activity) {
		if (activity == null) {
			return null;
		}
		Object workflowForm = null;
		String formClass = null;
		final AbstractTaskNode tasknode = taskNode(activity);
		if (tasknode instanceof UserNode) {
			formClass = ((UserNode) tasknode).getFormClass();
		}
		if (formClass == null) {
			formClass = ((ProcessNode) tasknode.parent()).getFormClass();
		}
		if (formClass != null) {
			try {
				workflowForm = formInstancCache.get(formClass);
				if (workflowForm == null) {
					formInstancCache.add(formClass, workflowForm = ClassUtils.newInstance(formClass));
				}
			} catch (final Exception e) {
				throw WorkflowException.of(e);
			}
		}
		return workflowForm;
	}

	@Override
	public boolean isFinalStatus(final ActivityBean activity) {
		final EActivityStatus status = activity.getStatus();
		return status == EActivityStatus.complete || status == EActivityStatus.abort;
	}

	ActivityBean createActivity(final AbstractTaskNode tasknode, final ActivityBean preActivity) {
		return createActivity(null, tasknode, preActivity);
	}

	ActivityBean createActivity(ProcessBean process, final AbstractTaskNode tasknode,
			final ActivityBean preActivity) {
		if (process == null && preActivity != null) {
			process = getProcessBean(preActivity);
		}
		final ActivityBean activity = createBean();
		activity.setProcessId(process.getId());
		if (preActivity != null) {
			activity.setPreviousId(preActivity.getId());
		}
		activity.setTasknodeId(tasknode.getId());
		activity.setTasknodeType(tasknode.getTasknodeType());
		activity.setCreateDate(new Date());
		return activity;
	}

	private final TaskExecutor taskExecutor = new TaskExecutor();

	{
		addListener(new TableEntityAdapterEx() {
			@Override
			public void afterDelete(final ITableEntityService manager, final IParamsValue paramsValue) {
				final Object[] activityIds = paramsValue.getValues();
				// 删除任务环节
				workitemMgr().deleteWith(SqlUtils.getIdsSQLParam("activityId", activityIds.length),
						activityIds);

				// 删除环节变量
				VariableManager.get().deleteVariables(EVariableSource.activity, activityIds);
			}
		});
	}
}
