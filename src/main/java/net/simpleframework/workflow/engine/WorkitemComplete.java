package net.simpleframework.workflow.engine;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.common.ID;
import net.simpleframework.common.ObjectEx;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.workflow.engine.IActivityManager.PropSequential;
import net.simpleframework.workflow.engine.participant.ParticipantUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class WorkitemComplete extends ObjectEx implements Serializable {
	public static final IWorkflowContext context = WorkflowContextFactory.get();

	private final ID workitemId;

	private boolean allCompleted = true;

	private WorkitemComplete(final WorkitemBean workitem) {
		workitemId = workitem.getId();

		// 判断是否所有的工作项都已完成
		final IWorkitemManager workitemMgr = context.getWorkitemMgr();
		final ActivityBean activity = workitemMgr.getActivity(workitem);

		if (PropSequential.list(activity).size() > 0) {
			allCompleted = false;
		} else {
			final int allWorkitems = workitemMgr.getWorkitemList(activity).getCount();
			// 完成的工作项
			final int complete = workitemMgr.getWorkitemList(activity, EWorkitemStatus.complete)
					.getCount();
			if (complete + 1 < ParticipantUtils.getResponseValue(
					context.getActivityMgr().taskNode(activity), allWorkitems)) {
				allCompleted = false;
			}
		}
	}

	public WorkitemBean getWorkitem() {
		return context.getWorkitemMgr().getBean(workitemId);
	}

	public boolean isAllCompleted() {
		return allCompleted;
	}

	private ActivityComplete activityComplete;

	public ActivityComplete getActivityComplete() {
		if (activityComplete == null) {
			activityComplete = new ActivityComplete(this);
		}
		return activityComplete;
	}

	public Object getWorkflowForm() {
		return context.getActivityMgr().getWorkflowForm(
				context.getWorkitemMgr().getActivity(getWorkitem()));
	}

	public void complete(final Map<String, String> parameters) {
		context.getWorkitemMgr().complete(parameters, this);
	}

	private static Map<ID, KVMap> variablesCache = new ConcurrentHashMap<ID, KVMap>();

	public KVMap getVariables() {
		KVMap kv = variablesCache.get(workitemId);
		if (kv == null) {
			variablesCache.put(workitemId, kv = new KVMap());
		}
		return kv;
	}

	public void done() {
		variablesCache.remove(workitemId);
	}

	public void reset() {
		completeCache.remove(workitemId);
	}

	private static Map<ID, WorkitemComplete> completeCache = new ConcurrentHashMap<ID, WorkitemComplete>();

	public static WorkitemComplete get(final WorkitemBean workitem) {
		final ID key = workitem.getId();
		WorkitemComplete workitemComplete = completeCache.get(key);
		if (workitemComplete == null) {
			completeCache.put(key, workitemComplete = new WorkitemComplete(workitem));
		}
		return workitemComplete;
	}

	private static final long serialVersionUID = 5112409107824255728L;
}
