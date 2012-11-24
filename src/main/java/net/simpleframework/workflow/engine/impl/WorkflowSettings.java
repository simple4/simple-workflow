package net.simpleframework.workflow.engine.impl;

import net.simpleframework.ctx.ContextSettings;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class WorkflowSettings extends ContextSettings {

	public static WorkflowSettings get() {
		return singleton(WorkflowSettings.class);
	}

	/**
	 * 获取当前服务器的地址
	 * 
	 * @return
	 */
	public String getServerUrl() {
		return "http://127.0.0.1:9090/streets2/hr";
	}

	/**
	 * 获取子流程的监控时间
	 * 
	 * @return
	 */
	public long getSubTaskPeriod() {
		return 60 * 5; // 单位秒
	}
}
