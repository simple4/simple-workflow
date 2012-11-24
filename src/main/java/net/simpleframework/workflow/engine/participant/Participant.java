package net.simpleframework.workflow.engine.participant;

import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.workflow.engine.WorkflowContextFactory;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class Participant {
	public ID userId;

	public ID roleId;

	public Participant(final ID userId, final ID roleId) {
		this.userId = userId;
		this.roleId = roleId != null ? roleId : WorkflowContextFactory.get().getParticipantMgr()
				.getRoleIdByUser(userId);
	}

	public Participant(final ID userId) {
		this(userId, null);
	}

	public String getId() {
		return roleId + "_" + userId;
	}

	@Override
	public String toString() {
		return userId + "," + roleId;
	}

	public static Participant of(final String participant) {
		final String[] pArr = StringUtils.split(participant, ",");
		return pArr.length == 2 ? new Participant(ID.Gen.id(pArr[0]), ID.Gen.id(pArr[1])) : null;
	}
}
