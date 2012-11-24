package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public enum EProcessStatus {
	running {

		@Override
		public String toString() {
			return $m("EProcessStatus.running");
		}
	},

	complete {

		@Override
		public String toString() {
			return $m("EProcessStatus.complete");
		}
	},

	suspended {

		@Override
		public String toString() {
			return $m("EProcessStatus.suspended");
		}
	},

	abort {

		@Override
		public String toString() {
			return $m("EProcessStatus.abort");
		}
	},

	timeout {

		@Override
		public String toString() {
			return $m("EProcessStatus.timeout");
		}
	}
}
