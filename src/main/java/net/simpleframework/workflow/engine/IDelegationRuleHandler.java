package net.simpleframework.workflow.engine;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IDelegationRuleHandler {

	/**
	 * 定义委托是否开始执行
	 * 
	 * @param delegation
	 * @return
	 */
	boolean isStart(DelegationBean delegation);

	/**
	 * 定义委托是否结束
	 * 
	 * @param delegation
	 * @return
	 */
	boolean isEnd(DelegationBean delegation);
}
