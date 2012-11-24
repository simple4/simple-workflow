package net.simpleframework.workflow.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.common.ClassUtils;
import net.simpleframework.common.ID;
import net.simpleframework.common.bean.AbstractIdBean;
import net.simpleframework.common.bean.BeanUtils;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.script.IScriptEval;
import net.simpleframework.common.script.ScriptEvalFactory;
import net.simpleframework.ctx.ado.AbstractBeanDbManager;
import net.simpleframework.workflow.engine.AbstractWorkflowBean;
import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.IWorkflowContext;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.WorkflowContextFactory;
import net.simpleframework.workflow.engine.event.IWorkflowListener;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class AbstractWorkflowManager<T extends AbstractIdBean> extends
		AbstractBeanDbManager<T, T> {
	static Collection<String> defaultExpr;
	static {
		defaultExpr = new ArrayList<String>();
		defaultExpr.add("import " + WorkflowContext.class.getPackage().getName() + ".*;");
	}

	public IScriptEval createScriptEval(final T bean) {
		final IScriptEval script = ScriptEvalFactory.createDefaultScriptEval(createVariables(bean));
		for (final String expr : defaultExpr) {
			script.eval(expr);
		}
		return script;
	}

	public KVMap createVariables(final T bean) {
		final KVMap variables = new KVMap();
		return variables;
	}

	public void assertStatus(final AbstractWorkflowBean bean, final Enum<?>... status) {
		final Enum<?> status2 = (Enum<?>) BeanUtils.getProperty(bean, "status");
		if (!ArrayUtils.contains(status, status2)) {
			throw WorkflowStatusException.of(status2, status);
		}
	}

	private final Map<ID, Set<String>> listenerClassMap = new ConcurrentHashMap<ID, Set<String>>();

	private static Map<String, IWorkflowListener> listenerInstanceMap = new ConcurrentHashMap<String, IWorkflowListener>();

	public Collection<IWorkflowListener> getEventListeners(final T bean) {
		final Set<String> set = new LinkedHashSet<String>();
		Set<String> set2 = listenerClassMap.get(bean.getId());
		if (set2 != null) {
			set.addAll(set2);
		}
		set2 = null;
		if (bean instanceof ProcessBean) {
			set2 = processMgr().processNode((ProcessBean) bean).listeners();
		} else if (bean instanceof ActivityBean) {
			set2 = activityMgr().taskNode((ActivityBean) bean).listeners();
		}
		if (set2 != null) {
			set.addAll(set2);
		}
		final ArrayList<IWorkflowListener> al = new ArrayList<IWorkflowListener>();
		for (final String listenerClass : set) {
			IWorkflowListener instance = listenerInstanceMap.get(listenerClass);
			if (instance == null) {
				listenerInstanceMap.put(listenerClass,
						instance = (IWorkflowListener) ClassUtils.newInstance(listenerClass));
			}
			al.add(instance);
		}
		return al;
	}

	public void addEventListener(final T bean, final Class<? extends IWorkflowListener> listenerClass) {
		final ID id = bean.getId();
		Set<String> set = listenerClassMap.get(id);
		if (set == null) {
			listenerClassMap.put(id, set = new LinkedHashSet<String>());
		}
		set.add(listenerClass.getName());
	}

	public boolean removeEventListener(final T bean,
			final Class<? extends IWorkflowListener> listenerClass) {
		final Set<String> set = listenerClassMap.get(bean.getId());
		if (set != null) {
			return set.remove(listenerClass.getName());
		}
		return false;
	}

	@Override
	public IWorkflowContext getModuleContext() {
		return WorkflowContextFactory.get();
	}

	protected ProcessModelManager modelMgr() {
		return (ProcessModelManager) getModuleContext().getModelMgr();
	}

	protected ProcessManager processMgr() {
		return (ProcessManager) getModuleContext().getProcessMgr();
	}

	protected ActivityManager activityMgr() {
		return (ActivityManager) getModuleContext().getActivityMgr();
	}

	protected WorkitemManager workitemMgr() {
		return (WorkitemManager) getModuleContext().getWorkitemMgr();
	}
}
