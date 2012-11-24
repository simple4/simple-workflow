package net.simpleframework.workflow.engine;

import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.script.IScriptEval;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IScriptAware<T extends AbstractWorkflowBean> {

	/**
	 * 
	 * @param bean
	 * @return
	 */
	IScriptEval createScriptEval(final T bean);

	/**
	 * 
	 * @param bean
	 * @return
	 */
	KVMap createVariables(final T bean);
}
