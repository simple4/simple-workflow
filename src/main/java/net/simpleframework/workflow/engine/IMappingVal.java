package net.simpleframework.workflow.engine;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IMappingVal {

	/**
	 * 获取映射变量的值。主要用在主-子流程的变量映射中
	 * 
	 * @param mapping
	 * @return
	 */
	Object val(String mapping);
}
