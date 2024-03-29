package net.simpleframework.workflow.engine;

import java.util.Date;
import java.util.Properties;

import net.simpleframework.common.ID;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ProcessBean extends AbstractWorkflowBean {
	private ID modelId;

	private Date completeDate;

	private String title;

	private EProcessStatus status;

	/**
	 * 流程启动者及启动角色
	 */
	private ID userId, roleId;

	private Properties properties;

	public ID getModelId() {
		return modelId;
	}

	public void setModelId(final ID modelId) {
		this.modelId = modelId;
	}

	public Date getCompleteDate() {
		return completeDate;
	}

	public void setCompleteDate(final Date completeDate) {
		this.completeDate = completeDate;
	}

	public EProcessStatus getStatus() {
		return status != null ? status : EProcessStatus.running;
	}

	public void setStatus(final EProcessStatus status) {
		this.status = status;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public ID getUserId() {
		return userId;
	}

	public void setUserId(final ID userId) {
		this.userId = userId;
	}

	public ID getRoleId() {
		return roleId;
	}

	public void setRoleId(final ID roleId) {
		this.roleId = roleId;
	}

	public Properties getProperties() {
		if (properties == null) {
			properties = new Properties();
		}
		return properties;
	}

	public void setProperties(final Properties properties) {
		this.properties = properties;
	}

	public static final String processId = "__process_Id";

	private static final long serialVersionUID = -4249661933122865392L;
}
