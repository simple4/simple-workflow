package net.simpleframework.workflow.engine;

import java.util.Collection;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IVariableAware<T extends AbstractWorkflowBean> {

	/**
	 * 获取变量的值
	 * 
	 * @param bean
	 * @param name
	 * @return
	 */
	Object getVariable(T bean, String name);

	/**
	 * 设置变量的值
	 * 
	 * @param bean
	 * @param name
	 * @param value
	 */
	void setVariable(T bean, String name, Object value);

	void setVariable(T bean, String[] names, Object[] values);

	/**
	 * 获取（流程、环节）所有定义的变量
	 * 
	 * @param bean
	 * @return
	 */
	Collection<String> getVariableNames(T bean);
}
