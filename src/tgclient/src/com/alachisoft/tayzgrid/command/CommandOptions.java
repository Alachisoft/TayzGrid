/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.alachisoft.tayzgrid.command;


public final class CommandOptions {

	public static final int COMMAND_SIZE = 10;

	public static final int DATA_SIZE = 10;

	public static final int TOTAL_SIZE = COMMAND_SIZE + DATA_SIZE;

	public static final String EXC_INITIAL = "EXCEPTION";

	/* Notification Modifier constants */

	/**
	 * This flag indicates that the Add notificatoin is enabled.
	 */
	static final int ADD_NOTIF_MASK = 1 << 0;

	/**
	 * This flag indicates that the Control key was down when the event
	 * occurred.
	 */
	public static final int UPDATE_NOTIF_MASK = 1 << 1;

	/**
	 * This flag indicates that the Meta key was down when the event occurred.
	 * For mouse events, this flag indicates that the right button was pressed
	 * or released.
	 */
	public static final int REMOVE_NOTIF_MASK = 1 << 2;

	/**
	 * This flag indicates that the Alt key was down when the event occurred.
	 * For mouse events, this flag indicates that the middle mouse button was
	 * pressed or released.
	 */
	public static final int CLEAR_NOTIF_MASK = 1 << 3;

}
