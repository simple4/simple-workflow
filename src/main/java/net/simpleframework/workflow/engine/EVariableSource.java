package net.simpleframework.workflow.engine;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public enum EVariableSource {

	/**
	 * 流程变量
	 */
	process,

	/**
	 * 环节变量
	 */
	activity,

	/**
	 * 流程变量静态类型
	 */
	model
}
