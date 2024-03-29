package net.simpleframework.workflow.engine.impl;

import static net.simpleframework.common.I18n.$m;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import net.simpleframework.ado.db.IQueryEntitySet;
import net.simpleframework.common.ID;
import net.simpleframework.common.ado.query.DataQueryUtils;
import net.simpleframework.common.ado.query.IDataQuery;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.DelegationBean;
import net.simpleframework.workflow.engine.EActivityAbortPolicy;
import net.simpleframework.workflow.engine.EActivityStatus;
import net.simpleframework.workflow.engine.EDelegationSource;
import net.simpleframework.workflow.engine.EProcessStatus;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IActivityManager.PropSequential;
import net.simpleframework.workflow.engine.IWorkflowForm;
import net.simpleframework.workflow.engine.IWorkitemManager;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.WorkitemComplete;
import net.simpleframework.workflow.engine.event.IActivityListener;
import net.simpleframework.workflow.engine.event.IWorkflowListener;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.engine.participant.ParticipantUtils;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.UserNode;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class WorkitemManager extends AbstractWorkflowManager<WorkitemBean> implements
		IWorkitemManager {
	@Override
	public ActivityBean getActivity(final WorkitemBean workitem) {
		return activityMgr().getBean(workitem.getActivityId());
	}

	@Override
	public void complete(final Map<String, String> parameters,
			final WorkitemComplete workitemComplete) {
		final WorkitemBean workitem = workitemComplete.getWorkitem();
		try {
			final ActivityManager activityMgr = activityMgr();
			assertStatus(workitem, EWorkitemStatus.running);
			final ActivityBean activity = getActivity(workitem);
			assertStatus(activity, EActivityStatus.running);
			final ProcessBean process = activityMgr.getProcessBean(activity);
			if (process.getStatus() == EProcessStatus.suspended) {
				throw WorkflowStatusException.of($m("WorkitemManager.2"));
			}

			final ProcessManager processMgr = processMgr();
			// 更新流程变量
			final KVMap variables = workitemComplete.getVariables();
			for (final Map.Entry<String, Object> e : variables.entrySet()) {
				final String key = e.getKey();
				if (activityMgr.getVariableNames(activity).contains(key)) {
					activityMgr.setVariable(activity, key, e.getValue());
				} else {
					processMgr.setVariable(process, key, e.getValue());
				}
			}

			workitem.setStatus(EWorkitemStatus.complete);
			workitem.setCompleteDate(new Date());
			update(new String[] { "completeDate", "status" }, workitem);

			if (workitemComplete.isAllCompleted()) {
				activityMgr.complete(workitemComplete.getActivityComplete());
			} else {
				final Collection<Participant> participants = PropSequential.list(activity);
				if (participants.size() > 0) { // 获取顺序执行的参与者
					final AbstractTaskNode tasknode = activityMgr.taskNode(activity);
					final Iterator<Participant> it = participants.iterator();
					if (ParticipantUtils.isInstanceShared(tasknode)) {
						insert(createWorkitem(activity, it.next()));
						PropSequential.set(activity, it);
						activityMgr.update(new String[] { "properties" }, activity);
					} else {
						final ActivityBean nActivity = activityMgr.createActivity(process, tasknode,
								activity);
						final Participant participant = it.next();
						PropSequential.set(nActivity, it);
						activityMgr.insert(nActivity);
						insert(createWorkitem(nActivity, participant));

						activity.setStatus(EActivityStatus.complete);
						activity.setCompleteDate(new Date());
						activityMgr.update(new String[] { "completeDate", "status" }, activity);
					}
				}
			}

			// 完成表单的complete
			final IWorkflowForm workflowForm = (IWorkflowForm) workitemComplete.getWorkflowForm();
			if (workflowForm != null) {
				workflowForm.onComplete(parameters, workitemComplete);
			}

			workitemComplete.done();

			// 事件
			for (final IWorkflowListener listener : activityMgr.getEventListeners(activity)) {
				((IActivityListener) listener).onWorkitemCompleted(workitemComplete);
			}
		} finally {
			workitemComplete.reset();
		}
	}

	@Override
	public void retake(final WorkitemBean workitem) {
		assertStatus(workitem, EWorkitemStatus.complete);
		final ActivityBean activity = getActivity(workitem);
		final ActivityManager activityMgr = activityMgr();
		final ProcessBean process = activityMgr.getProcessBean(activity);
		assertStatus(process, EProcessStatus.running);

		final EActivityStatus status = activity.getStatus();
		if (status == EActivityStatus.complete) {
			// 检测后续环节是否合法
			final IDataQuery<ActivityBean> qs = activityMgr.getNextActivities(activity);
			ActivityBean nextActivity;
			while ((nextActivity = qs.next()) != null) {
				if (!(activityMgr.taskNode(nextActivity) instanceof UserNode)) {
					throw WorkflowException.of($m("WorkitemManager.0"));
				}
				WorkitemBean workitem2;
				final IDataQuery<WorkitemBean> qs2 = getWorkitemList(nextActivity);
				while ((workitem2 = qs2.next()) != null) {
					if (workitem2.isReadMark() || workitem2.getStatus() != EWorkitemStatus.running) {
						throw WorkflowException.of($m("WorkitemManager.1"));
					}
				}
				activityMgr.abort(nextActivity, EActivityAbortPolicy.nextActivities);
			}

			activity.setStatus(EActivityStatus.running);
			activity.setCompleteDate(null);
			activityMgr.update(new String[] { "status", "completeDate" }, activity);
		} else if (status == EActivityStatus.running) {
			// 顺序，单实例

			if (ParticipantUtils.isSequential(activityMgr.taskNode(activity))) {
				final IDataQuery<WorkitemBean> qs = getWorkitemList(activity, EWorkitemStatus.running);
				WorkitemBean workitem2;
				if ((workitem2 = qs.next()) != null) {
					if (workitem2.isReadMark()) {
						throw WorkflowException.of($m("WorkitemManager.1"));
					}
					workitem2.setStatus(EWorkitemStatus.abort);
					workitem2.setCompleteDate(new Date());
					update(new String[] { "status", "completeDate" }, workitem2);

					PropSequential.push(activity,
							new Participant(workitem2.getUserId(), workitem2.getRoleId()));
					activityMgr.update(new String[] { "properties" }, activity);
				}
			}
		} else {
			throw WorkflowStatusException
					.of(status, EActivityStatus.running, EActivityStatus.complete);
		}
		if (status == EActivityStatus.complete || status == EActivityStatus.running) {
			workitem.setStatus(EWorkitemStatus.running);
			workitem.setCompleteDate(null);
			update(new String[] { "status", "completeDate" }, workitem);
		}

		// 事件
		for (final IWorkflowListener listener : activityMgr.getEventListeners(activity)) {
			((IActivityListener) listener).onWorkitemRetake(workitem);
		}
	}

	@Override
	public void readMark(final WorkitemBean workitem, final boolean unread) {
		assertStatus(workitem, EWorkitemStatus.running);
		workitem.setReadMark(!unread);
		update(new String[] { "readMark" }, workitem);
	}

	@Override
	public void setWorkitemDelegation(final WorkitemBean workitem, final DelegationBean delegation) {
		delegation.setDelegationSource(EDelegationSource.workitem);
		delegation.setSourceId(workitem.getId());

		// insert(delegation);
	}

	private IQueryEntitySet<WorkitemBean> _getWorkitemList(final ID id, final String field,
			final EWorkitemStatus... status) {
		final StringBuilder sql = new StringBuilder();
		sql.append(field).append("=?");
		final ArrayList<Object> params = new ArrayList<Object>();
		params.add(id);
		if (status != null && status.length > 0) {
			sql.append(" and (");
			int i = 0;
			for (final EWorkitemStatus s : status) {
				if (i++ > 0) {
					sql.append(" or ");
				}
				sql.append("status=?");
				params.add(s);
			}
			sql.append(")");
		}
		return query(sql.toString(), params.toArray());
	}

	@Override
	public IDataQuery<WorkitemBean> getWorkitemList(final ActivityBean activity,
			final EWorkitemStatus... status) {
		if (activity == null) {
			return DataQueryUtils.nullQuery();
		}
		return _getWorkitemList(activity.getId(), "activityId", status);
	}

	@Override
	public IDataQuery<WorkitemBean> getWorkitemList(final ID userId, final EWorkitemStatus... status) {
		if (userId == null) {
			return DataQueryUtils.nullQuery();
		}
		return _getWorkitemList(userId, "userId", status);
	}

	@Override
	public KVMap createVariables(final WorkitemBean workitem) {
		final KVMap variables = activityMgr().createVariables(getActivity(workitem));
		variables.add("workitem", workitem);
		return variables;
	}

	@Override
	public boolean isFinalStatus(final WorkitemBean workitem) {
		final EWorkitemStatus status = workitem.getStatus();
		return status == EWorkitemStatus.complete || status == EWorkitemStatus.abort;
	}

	WorkitemBean createWorkitem(final ActivityBean activity, final Participant participant) {
		final WorkitemBean workitem = createBean();
		workitem.setActivityId(activity.getId());
		workitem.setUserId(participant.userId);
		workitem.setCreateDate(activity.getCreateDate());
		workitem.setRoleId(participant.roleId);
		return workitem;
	}
}
