/*
 * Copyright IBM Corp. and others 2010
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
package com.ibm.j9ddr.autoblob.datamodel;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import com.ibm.j9ddr.autoblob.linenumbers.SourceLocation;

/**
 * @author andhall
 *
 */
public class StructType extends RecordType
{
	private List<EnumType> innerEnums = new LinkedList<EnumType>();
	
	public StructType(String name, SourceLocation location)
	{
		super(name, location);
	}

	@Override
	public String getFullName()
	{
		if (isAnonymous()) {
			return "anonymous struct";
		} else {
			return "struct " + getName();
		}
	}

	@Override
	protected String getBlobStructName()
	{
		if (isAnonymous()) {
			return "AnonymousStruct";
		} else {
			String name = getName();
			
			return name.replaceAll("::","__");
		}
	}

	@Override
	protected void writeConstantEntries(PrintWriter out, PrintWriter ssout)
	{
		super.writeConstantEntries(out, ssout);
		
		for (EnumType innerType : innerEnums) {
			innerType.setRValuePrefix(this.getName() + "::");
			innerType.writeConstantEntries(out, ssout);
		}
	}

	@Override
	public void attachInnerEnum(EnumType type)
	{
		innerEnums.add(type);
	}
}
