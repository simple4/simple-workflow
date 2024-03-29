package net.simpleframework.workflow.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import net.simpleframework.common.StringUtils;
import net.simpleframework.common.ado.query.IDataQuery;
import net.simpleframework.ctx.ado.IBeanManagerAware;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.schema.AbstractTaskNode;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IActivityManager extends IVariableAware<ActivityBean>,
		IBeanManagerAware<ActivityBean>, IListenerAware<ActivityBean>, IScriptAware<ActivityBean> {

	/**
	 * 获取流程实例对象
	 * 
	 * @param activity
	 * @return
	 */
	ProcessBean getProcessBean(ActivityBean activity);

	/**
	 * 获取当前环节的节点描述
	 * 
	 * @param activity
	 * @return
	 */
	AbstractTaskNode taskNode(ActivityBean activity);

	/**
	 * 获取指定流程实例下的所有环节实例，默认按创建日期排序
	 * 
	 * @param processBean
	 * @return
	 */
	IDataQuery<ActivityBean> getActivities(ProcessBean processBean, EActivityStatus... status);

	/**
	 * 获取指定环节的后续环节实例
	 * 
	 * @param preActivity
	 * @return
	 */
	IDataQuery<ActivityBean> getNextActivities(ActivityBean preActivity);

	/**
	 * 
	 * @param activity
	 * @return
	 */
	ActivityBean getPreActivity(ActivityBean activity);

	/**
	 * 获取前一指定环节，获取的环节必须在创建链路上
	 * 
	 * @param activity
	 * @param tasknode
	 *           环节id或名称
	 * @return
	 */
	ActivityBean getPreActivity(ActivityBean activity, String tasknode);

	/**
	 * 
	 * @param processBean
	 * @return
	 */
	ActivityBean getStartActivity(ProcessBean processBean);

	/**
	 * 
	 * @param activityCallback
	 */
	void complete(ActivityComplete activityCallback);

	/**
	 * 
	 * @param activity
	 * @param resume
	 */
	void suspend(ActivityBean activity, boolean resume);

	/**
	 * 
	 * @param activity
	 * @param policy
	 *           放弃策略
	 */
	void abort(ActivityBean activity, EActivityAbortPolicy policy);

	/**
	 * 跳转到指定的任务环节
	 * 
	 * 跳转和回退的区别：跳转可以到任何一个有效的任务环节，按照模型创建参与者；回退只能是已运行过的任务，按运行历史创建参与者
	 * 
	 * @param activity
	 * @param tasknode
	 *           环节id或名称
	 */
	void jump(ActivityBean activity, String tasknode);

	/**
	 * 回退到指定的任务环节
	 * 
	 * @param activity
	 * @param tasknode
	 *           环节id或名称
	 */
	void fallback(ActivityBean activity, String tasknode);

	void fallback(ActivityBean activity);

	/**
	 * 运行远程子流程
	 * 
	 * @param activity
	 */
	void doRemoteSubTask(ActivityBean activity);

	/**
	 * 完成子流程环节
	 * 
	 * @param activity
	 * @param mappingVal
	 */
	void subComplete(ActivityBean activity, IMappingVal mappingVal);

	/**
	 * 获取表单实例
	 * 
	 * @param activity
	 * @return
	 */
	Object getWorkflowForm(ActivityBean activity);

	/**
	 * 是否最终状态，不可状态转换
	 * 
	 * @param activity
	 * @return
	 */
	boolean isFinalStatus(ActivityBean activity);

	public abstract static class PropSequential {

		private static final String KEY = "sequential_participants";

		public static Collection<Participant> list(final ActivityBean activity) {
			final ArrayList<Participant> participants = new ArrayList<Participant>();
			final String[] pArr = StringUtils.split(activity.getProperties().getProperty(KEY), ";");
			if (pArr != null) {
				Participant participant;
				for (final String str : pArr) {
					if ((participant = Participant.of(str)) != null) {
						participants.add(participant);
					}
				}
			}
			return participants;
		}

		public static void set(final ActivityBean activity, final Iterator<Participant> it) {
			if (it == null) {
				return;
			}

			final StringBuilder sb = new StringBuilder();
			int i = 0;
			while (it.hasNext()) {
				final Participant participant = it.next();
				if (i++ > 0) {
					sb.append(";");
				}
				sb.append(participant);
			}

			final Properties properties = activity.getProperties();
			if (sb.length() > 0) {
				properties.put(KEY, sb.toString());
			} else {
				properties.remove(KEY);
			}
		}

		public static void push(final ActivityBean activity, final Participant participant) {
			String nstr = participant.toString();
			final Properties properties = activity.getProperties();
			final String ostr = properties.getProperty(KEY);
			if (StringUtils.hasText(ostr)) {
				nstr += ";" + ostr;
			}
			properties.setProperty(KEY, nstr);
		}
	}
}
