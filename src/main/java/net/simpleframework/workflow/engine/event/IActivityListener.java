package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EActivityAbortPolicy;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.WorkitemComplete;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IActivityListener extends IWorkflowListener {

	/**
	 * 环节完成时触发
	 * 
	 * @param activityComplete
	 */
	void onCompleted(ActivityComplete activityComplete);

	/**
	 * 环节放弃时触发
	 * 
	 * @param activity
	 * @param policy
	 */
	void onAbort(ActivityBean activity, EActivityAbortPolicy policy);

	/**
	 * 环节挂起或恢复时触发
	 * 
	 * @param activity
	 */
	void onSuspend(ActivityBean activity);

	/**
	 * 环节回退时触发
	 * 
	 * @param activity
	 * @param tasknode
	 */
	void onFallback(ActivityBean activity, String tasknode);

	/**
	 * 
	 * @param workitemComplete
	 */
	void onWorkitemCompleted(WorkitemComplete workitemComplete);

	/**
	 * 
	 * @param workitem
	 */
	void onWorkitemRetake(WorkitemBean workitem);
}
