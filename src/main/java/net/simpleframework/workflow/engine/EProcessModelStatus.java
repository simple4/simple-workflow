package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public enum EProcessModelStatus {
	edit {

		@Override
		public String toString() {
			return $m("EProcessModelStatus.edit");
		}
	},

	deploy {

		@Override
		public String toString() {
			return $m("EProcessModelStatus.deploy");
		}
	},

	abort {

		@Override
		public String toString() {
			return $m("EProcessModelStatus.abort");
		}
	}
}
