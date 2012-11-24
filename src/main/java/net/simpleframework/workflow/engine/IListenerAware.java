package net.simpleframework.workflow.engine;

import java.util.Collection;

import net.simpleframework.workflow.engine.event.IWorkflowListener;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IListenerAware<T extends AbstractWorkflowBean> {

	/**
	 * 
	 * @param bean
	 * @param listenerClass
	 */
	void addEventListener(T bean, Class<? extends IWorkflowListener> listenerClass);

	/**
	 * 
	 * @param bean
	 * @param listenerClass
	 * @return
	 */
	boolean removeEventListener(T bean, Class<? extends IWorkflowListener> listenerClass);

	/**
	 * 
	 * @param bean
	 * @return
	 */
	Collection<IWorkflowListener> getEventListeners(T bean);
}
