package net.simpleframework.workflow.engine.impl;

import net.simpleframework.workflow.engine.DelegationBean;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class DelegationManager extends AbstractWorkflowManager<DelegationBean> {
	private static DelegationManager dm = new DelegationManager();

	static DelegationManager get() {
		return dm;
	}
}
