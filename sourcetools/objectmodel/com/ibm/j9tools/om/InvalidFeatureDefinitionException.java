/*
 * Copyright IBM Corp. and others 2007
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] https://openjdk.org/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0 OR GPL-2.0-only WITH OpenJDK-assembly-exception-1.0
 */
package com.ibm.j9tools.om;

import java.util.Collection;

/**
 * @author	Gabriel Castro
 */
public class InvalidFeatureDefinitionException extends OMObjectException {
	private static final long serialVersionUID = 1L;

	/** Identifier for serialized instances. */

	/**
	 * @param 	cause
	 */
	public InvalidFeatureDefinitionException(Throwable cause, String objectId) {
		super(cause, objectId);
	}

	/**
	 * @param 	causes
	 */
	public InvalidFeatureDefinitionException(Collection<Throwable> causes, OMObject object) {
		super(causes, object);
	}

	/**
	 * @param	errors
	 * @param 	warnings
	 */
	public InvalidFeatureDefinitionException(Collection<Throwable> errors, Collection<Throwable> warnings, String objectId) {
		super(errors, warnings, objectId);
	}

	/**
	 * @see com.ibm.j9tools.om.OMObjectException#getObject()
	 */
	@Override
	public FeatureDefinition getObject() {
		return (FeatureDefinition) super.getObject();
	}

	/**
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder(Messages.getString("InvalidFeatureDefinitionException.0") + " \"");  //$NON-NLS-1$//$NON-NLS-2$

		if (object == null) {
			sb.append(objectId);
		} else {
			sb.append(((FeatureDefinition) object).getId());
		}

		sb.append("\":\n"); //$NON-NLS-1$

		for (Throwable e : errors) {
			sb.append("    - "); //$NON-NLS-1$
			sb.append(e.getMessage());
			sb.append("\n"); //$NON-NLS-1$
		}

		return sb.toString();
	}
}
