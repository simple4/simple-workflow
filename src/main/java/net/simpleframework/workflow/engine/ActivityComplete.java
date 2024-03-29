package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.simpleframework.common.ClassUtils;
import net.simpleframework.common.ID;
import net.simpleframework.common.ObjectEx;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.script.IScriptEval;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.participant.IParticipants;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.engine.participant.ParticipantUtils;
import net.simpleframework.workflow.schema.AbstractParticipantType;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.UserNode;
import net.simpleframework.workflow.schema.UserNode.Role;
import net.simpleframework.workflow.schema.UserNode.RuleRole;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ActivityComplete extends ObjectEx implements Serializable {
	public static final IWorkflowContext context = WorkflowContextFactory.get();

	private WorkitemComplete workitemComplete;

	private ID activityId;

	private final Map<String, TransitionNode> _transitions;

	private final Map<String, Collection<Participant>> _participants;
	{
		_transitions = new LinkedHashMap<String, TransitionNode>();
		_participants = new LinkedHashMap<String, Collection<Participant>>();
	}

	public ActivityComplete(final WorkitemComplete workitemComplete) {
		this.workitemComplete = workitemComplete;
		doInit(context.getWorkitemMgr().getActivity(workitemComplete.getWorkitem()));
	}

	public ActivityComplete(final ActivityBean activity) {
		doInit(activity);
	}

	/**
	 * 指定的转移构造，用在流程启动
	 * 
	 * @param activity
	 *           开始环节
	 * @param transitions
	 */
	public ActivityComplete(final ActivityBean activity, final Collection<TransitionNode> transitions) {
		activityId = activity.getId();

		final IScriptEval script = context.getActivityMgr().createScriptEval(activity);
		for (final TransitionNode transition : transitions) {
			_transitions.put(transition.getId(), transition);
			putParticipant(transition, script);
		}
	}

	private void doInit(final ActivityBean activity) {
		activityId = activity.getId();

		final AbstractTaskNode tasknode = context.getActivityMgr().taskNode(activity);
		final IScriptEval script;
		if (workitemComplete != null) {
			script = context.getWorkitemMgr().createScriptEval(workitemComplete.getWorkitem());
			final KVMap variables = workitemComplete.getVariables();
			for (final Map.Entry<String, Object> e : variables.entrySet()) {
				script.putVariable(e.getKey(), e.getValue());
			}
		} else {
			script = context.getActivityMgr().createScriptEval(activity);
		}

		// 解析条件正确的transition
		TransitionUtils.doTransitions(tasknode, script, _transitions);

		// 解析jobs
		for (final TransitionNode transition : getTransitions()) {
			putParticipant(transition, script);
		}
	}

	private void putParticipant(final TransitionNode transition, final IScriptEval script) {
		final AbstractTaskNode toTask = transition.to();
		if (!(toTask instanceof UserNode)) {
			return;
		}
		final KVMap variables = new KVMap().add("activityComplete", this).add("transition",
				transition);
		final AbstractParticipantType pt = ((UserNode) toTask).getParticipantType();
		final ArrayList<Participant> participants = new ArrayList<Participant>();
		if (pt instanceof RuleRole) {
			try {
				final IParticipants hdl = (IParticipants) ClassUtils.forName(
						((UserNode) toTask).getParticipantType().getParticipant()).newInstance();
				final Collection<Participant> _participants = hdl.participants(script, variables);
				if (_participants != null) {
					participants.addAll(_participants);
				}
			} catch (final Exception e) {
				throw WorkflowException.of(e);
			}
		} else {
			final IParticipants hdl = ParticipantUtils.hdlMap.get(pt.getClass());
			Collection<Participant> _participants;
			if (hdl != null && (_participants = hdl.participants(script, variables)) != null) {
				participants.addAll(_participants);
			}
		}

		if (participants.size() > 0) {
			_participants.put(transition.getId(), participants);
		} else {
			throw WorkflowException.of($m("ActivityComplete.0"));
		}
	}

	public WorkitemComplete getWorkitemComplete() {
		return workitemComplete;
	}

	public ActivityBean getActivity() {
		return context.getActivityMgr().getBean(activityId);
	}

	public void complete() {
		context.getActivityMgr().complete(this);
	}

	public boolean isTransitionManual() {
		for (final TransitionNode transition : getTransitions()) {
			if (TransitionUtils.isTransitionManual(transition)) {
				return true;
			}
		}
		return false;
	}

	public boolean isParticipantManual(final String[] transitionIds) {
		Collection<TransitionNode> values;
		if (transitionIds != null) {
			values = new ArrayList<TransitionNode>();
			for (final String id : transitionIds) {
				final TransitionNode transition = getTransitionById(id);
				if (transition != null) {
					values.add(transition);
				}
			}
		} else {
			values = _transitions.values();
		}
		for (final TransitionNode transition : values) {
			final AbstractTaskNode node = transition.to();
			if (node instanceof UserNode && isParticipantManual((UserNode) node)) {
				return true;
			}
		}
		return false;
	}

	public boolean isParticipantManual() {
		return isParticipantManual((String[]) null);
	}

	public boolean isParticipantManual(final UserNode usernode) {
		if (usernode == null) {
			return false;
		}
		final AbstractParticipantType pt = usernode.getParticipantType();
		return pt instanceof Role && ((Role) pt).isManual();
	}

	public Collection<TransitionNode> getTransitions() {
		return _transitions.values();
	}

	public Collection<Participant> getParticipants(final TransitionNode transition) {
		return _participants.get(transition.getId());
	}

	public TransitionNode getTransitionById(final String id) {
		return _transitions.get(id);
	}

	public void resetTransitions(final String[] transitionIds) {
		TransitionUtils.resetTransitions(transitionIds, _transitions);
	}

	public void resetParticipants(final Map<String, String[]> participantIds) {
		if (participantIds == null) {
			return;
		}
		final Set<String> keys = participantIds.keySet();
		resetTransitions(keys.toArray(new String[keys.size()]));

		final Map<String, Collection<Participant>> participants = new LinkedHashMap<String, Collection<Participant>>();
		for (final TransitionNode transition : getTransitions()) {
			final ArrayList<Participant> al = new ArrayList<Participant>();
			for (final String id : participantIds.get(transition.getId())) {
				for (final Participant participant : getParticipants(transition)) {
					if (id.equals(participant.getId())) {
						al.add(participant);
						break;
					}
				}
			}
			participants.put(transition.getId(), al);
		}
		_participants.clear();
		_participants.putAll(participants);
	}

	private static final long serialVersionUID = -3129141441786781334L;
}
