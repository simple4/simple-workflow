package net.simpleframework.workflow.engine;

import java.util.LinkedHashMap;
import java.util.Map;

import net.simpleframework.common.Convert;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.script.IScriptEval;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.AbstractTransitionType;
import net.simpleframework.workflow.schema.AbstractTransitionType.Conditional;
import net.simpleframework.workflow.schema.AbstractTransitionType.LogicConditional;
import net.simpleframework.workflow.schema.ETransitionLogic;
import net.simpleframework.workflow.schema.TransitionNode;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class TransitionUtils {
	public static boolean isTransitionManual(final TransitionNode transition) {
		if (transition != null) {
			final AbstractTransitionType tt = transition.getTransitionType();
			if (tt instanceof Conditional) {
				return ((Conditional) tt).isManual();
			} else if (tt instanceof LogicConditional) {
				return ((LogicConditional) tt).isManual();
			}
		}
		return false;
	}

	public static void doTransitions(final AbstractTaskNode tasknode, final IScriptEval script,
			final Map<String, TransitionNode> _transitions) {
		final LinkedHashMap<String, TransitionNode> logicTransitions = new LinkedHashMap<String, TransitionNode>();
		for (final TransitionNode transition : tasknode.toTransitions()) {
			final AbstractTransitionType tt = transition.getTransitionType();
			if (tt instanceof Conditional) {
				final String expr = ((Conditional) tt).getExpression();
				if (StringUtils.hasText(expr)) {
					script.putVariable("transition", transition);
					if (Convert.toBool(script.eval(expr))) {
						_transitions.put(transition.getId(), transition);
					}
				} else {
					_transitions.put(transition.getId(), transition);
				}
			} else if (tt instanceof AbstractTransitionType.Interface) {
				//
			} else if (tt instanceof LogicConditional) {
				logicTransitions.put(transition.getId(), transition);
			}
		}
		while (logicTransitions.size() > 0) {
			final TransitionNode transition = logicTransitions.remove(0);
			final LogicConditional lc = (LogicConditional) transition.getTransitionType();
			final String id2 = lc.getTransitionId();
			final ETransitionLogic logic = lc.getLogic();
			final TransitionNode transition2 = _transitions.get(id2);
			if (transition2 == null && logicTransitions.get(id2) != null) {
				logicTransitions.put(transition.getId(), transition);
				continue;
			}
			if ((logic == ETransitionLogic.and && transition2 != null)
					|| (logic == ETransitionLogic.not && transition2 == null)) {
				_transitions.put(transition.getId(), transition);
			}
		}
	}

	public static void resetTransitions(final String[] transitionIds,
			final Map<String, TransitionNode> _transitions) {
		if (transitionIds == null) {
			return;
		}
		final Map<String, TransitionNode> transitions = new LinkedHashMap<String, TransitionNode>();
		for (final String id : transitionIds) {
			final TransitionNode transition = _transitions.get(id);
			if (transition != null) {
				transitions.put(transition.getId(), transition);
			}
		}
		_transitions.clear();
		_transitions.putAll(transitions);
	}
}
