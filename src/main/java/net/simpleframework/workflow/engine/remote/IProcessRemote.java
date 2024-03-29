package net.simpleframework.workflow.engine.remote;

import net.simpleframework.workflow.remote.IWorkflowRemote;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IProcessRemote extends IWorkflowRemote {
	// 定义环节或流程的属性key值

	/**
	 * 主流程的服务url
	 */
	public static final String SERVERURL = "server_url";

	/**
	 * 模型名或id
	 */
	public static final String MODEL = "model";

	/**
	 * 子流程环节的id
	 */
	public static final String SUB_ACTIVITYID = "sub_activityId";

	/**
	 * 子流程id
	 */
	public static final String SUB_PROCESSID = "sub_processId";

	/**
	 * 映射的流程变量
	 */
	public static final String VAR_MAPPINGS = "var_mappings";
}
