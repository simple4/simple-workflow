package net.simpleframework.workflow.engine;

import java.util.Map;

import net.simpleframework.common.ID;
import net.simpleframework.common.ado.query.IDataQuery;
import net.simpleframework.ctx.ado.IBeanManagerAware;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IWorkitemManager extends IBeanManagerAware<WorkitemBean>,
		IListenerAware<WorkitemBean>, IScriptAware<WorkitemBean> {

	/**
	 * 获取环节实例
	 * 
	 * @param workitem
	 * @return
	 */
	ActivityBean getActivity(WorkitemBean workitem);

	/**
	 * 获取某一环节的所有工作列表
	 * 
	 * @param activity
	 * @param status
	 * @return
	 */
	IDataQuery<WorkitemBean> getWorkitemList(ActivityBean activity, EWorkitemStatus... status);

	IDataQuery<WorkitemBean> getWorkitemList(ID userId, EWorkitemStatus... status);

	/**
	 * 完成当前的工作项
	 * 
	 * @param workitemComplete
	 */
	void complete(Map<String, String> parameters, WorkitemComplete workitemComplete);

	/**
	 * 取回当前的工作项
	 * 
	 * @param workitem
	 */
	void retake(WorkitemBean workitem);

	/**
	 * 设置已读或标记未读
	 * 
	 * @param workitem
	 * @param unread
	 *           true标记为未读
	 */
	void readMark(WorkitemBean workitem, boolean unread);

	/**
	 * 设置工作项委托
	 * 
	 * @param workitem
	 * @param delegation
	 */
	void setWorkitemDelegation(WorkitemBean workitem, DelegationBean delegation);

	/**
	 * 是否最终状态，不可状态转换
	 * 
	 * @param workitem
	 * @return
	 */
	boolean isFinalStatus(WorkitemBean workitem);
}
