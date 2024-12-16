/*
 * Copyright IBM Corp. and others 2017
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

package org.openj9.test.ivj;

public class Hanoi {

	private Post[] posts;
	private int numberOfDisks;

	public Hanoi(int numberOfDisks) {
		this.numberOfDisks = numberOfDisks;
		this.posts = new Post[3];
		for (int i = 0; i < 3; ++i)
			this.posts[i] = new Post("" + i, numberOfDisks);
	}

	public int getNumberOfDisks() {
		return this.numberOfDisks;
	}

	public Post[] getPosts() {
		return this.posts;
	}
	public static void main(String[] args) {
		int numberOfDisks = 0;
		boolean inputError = false;

		if (args.length > 0) {
			try {
				numberOfDisks = Integer.parseInt(args[0]);
			} catch (NumberFormatException localNumberFormatException) {
				inputError = true;
			}
		}
		if ((inputError | numberOfDisks < 1)) {
			System.out.println("Invalid argument");
			numberOfDisks = 4;
		}

		System.out.println("Beginning puzzle.  Solving for " + numberOfDisks + " disks.");
		Hanoi puzzle = new Hanoi(numberOfDisks);
		puzzle.solve();
		System.out.println("Puzzle solved!");
	}
	
	private void moveDisk(Post source, Post destination) {
		destination.addDisk(source.removeDisk());
		reportMove(source, destination);
	}

	protected void reportMove(Post source, Post destination) {
		System.out.println("Moved disk " + source.getLabel() + " to " + destination.getLabel());
	}

	private void reset() {
		this.posts[0].loadDisks();
		this.posts[1].unloadDisks();
		this.posts[2].unloadDisks();
	}

	public void solve() {
		reset();
		solve(this.numberOfDisks, this.posts[0], this.posts[1], this.posts[2]);
	}

	private void solve(int depth, Post start, Post free, Post end) {
		if (depth == 1) {
			moveDisk(start, end);
		} else if (depth > 1) {
			solve(depth - 1, start, end, free);
			moveDisk(start, end);
			solve(depth - 1, free, start, end);
		}
	}
}
