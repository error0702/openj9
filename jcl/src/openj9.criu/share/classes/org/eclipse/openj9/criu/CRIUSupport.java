/*[INCLUDE-IF CRIU_SUPPORT]*/
/*******************************************************************************
 * Copyright (c) 2021, 2021 IBM Corp. and others
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
 * [2] http://openjdk.java.net/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR LicenseRef-GPL-2.0 WITH Assembly-exception
 *******************************************************************************/
package org.eclipse.openj9.criu;

import java.nio.file.Path;
/*[IF JAVA_SPEC_VERSION < 17] */
import java.security.AccessController;
/*[ENDIF] JAVA_SPEC_VERSION < 17 */
import java.security.PrivilegedAction;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/*[IF JAVA_SPEC_VERSION == 8] */
import sun.misc.Unsafe;
/*[ELSE]
import jdk.internal.misc.Unsafe;
/*[ENDIF] JAVA_SPEC_VERSION == 8 */
import java.util.Objects;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * CRIU Support API
 */
public final class CRIUSupport {

	@SuppressWarnings("restriction")
	private static Unsafe unsafe;

	private static final CRIUDumpPermission CRIU_DUMP_PERMISSION = new CRIUDumpPermission();

	private static boolean criuSupportEnabled = false;

	private static native boolean isCRIUSupportEnabledImpl();

	private static native boolean isCheckpointAllowed();

	private static boolean nativeLoaded = false;

	private static boolean initComplete = false;

	private static native void checkpointJVMImpl(String imageDir,
			boolean leaveRunning,
			boolean shellJob,
			boolean extUnixSupport,
			int logLevel,
			String logFile,
			boolean fileLocks,
			String workDir,
			boolean tcpEstablished,
			boolean autoDedup,
			boolean trackMemory);

	/**
	 * Constructs a new {@code CRIUSupport}.
	 *
	 * The default CRIU dump options are:
	 * <p>
	 * {@code imageDir} = imageDir, the directory where the images are to be
	 * created.
	 * <p>
	 * {@code leaveRunning} = false
	 * <p>
	 * {@code shellJob} = false
	 * <p>
	 * {@code extUnixSupport} = false
	 * <p>
	 * {@code logLevel} = 2
	 * <p>
	 * {@code logFile} = criu.log
	 * <p>
	 * {@code fileLocks} = false
	 * <p>
	 * {@code workDir} = imageDir, the directory where the images are to be created.
	 *
	 * @param imageDir the directory that will hold the dump files as a
	 *                 java.nio.file.Path
	 * @throws NullPointerException     if imageDir is null
	 * @throws SecurityException        if no permission to access imageDir or no
	 *                                  CRIU_DUMP_PERMISSION
	 * @throws IllegalArgumentException if imageDir is not a valid directory
	 */
	public CRIUSupport(Path imageDir) {
		/* [IF JAVA_SPEC_VERSION < 17] */
		@SuppressWarnings({"deprecation" })
		/* [ENDIF] JAVA_SPEC_VERSION < 17 */
		SecurityManager manager = System.getSecurityManager();
		if (manager != null) {
			manager.checkPermission(CRIU_DUMP_PERMISSION);
		}

		setImageDir(imageDir);
	}

	@SuppressWarnings({ "restriction", "deprecation" })
	private static synchronized void init() {
		if (!initComplete) {
			if (loadNativeLibrary()) {
				criuSupportEnabled = isCRIUSupportEnabledImpl();
				AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
					try {
						Field f = Unsafe.class.getDeclaredField("theUnsafe"); //$NON-NLS-1$
						f.setAccessible(true);
						unsafe = (Unsafe) f.get(null);
					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
						throw new InternalError(e);
					}
					return null;
				});

			}

			initComplete = true;
		}
	}

	/**
	 * Queries if CRIU support is enabled.
	 *
	 * @return TRUE is support is enabled, FALSE otherwise
	 */
	public synchronized static boolean isCRIUSupportEnabled() {
		if (!initComplete) {
			init();
		}

		return criuSupportEnabled;
	}

	/**
	 * Returns an error message describing why isCRIUSupportEnabled()
	 * returns false, and what can be done to remediate the issue.
	 *
	 * @return NULL if isCRIUSupportEnabled() returns true. Otherwise the error message
	 */
	public static String getErrorMessage() {
		String s = null;
		if (!isCRIUSupportEnabled()) {
			if (nativeLoaded) {
				s = "To enable criu support, please run java with the `-XX:+EnableCRIUSupport` option.";
			} else {
				s = "There was a problem loaded the criu native library.\n"
						+ "Please check that criu is installed on the machine by running `criu check`.\n"
						+ "Also, please ensure that the JDK is criu enabled by contacting your JDK provider.";
			}
		}
		return s;
	}
	/* Higher priority hooks are run last in pre-checkoint hooks, and are run
	 * first in post restore hooks.
	 */
	private static final int RESTORE_ENVIRONMENT_VARIABLES_PRIORITY = 100;
	private static final int USER_HOOKS_PRIORITY = 1;

	private String imageDir;
	private boolean leaveRunning;
	private boolean shellJob;
	private boolean extUnixSupport;
	private int logLevel;
	private String logFile;
	private boolean fileLocks;
	private String workDir;
	private boolean tcpEstablished;
	private boolean autoDedup;
	private boolean trackMemory;
	private Path envFile;

	@SuppressWarnings("deprecation")
	private synchronized static boolean loadNativeLibrary() {
		if (!nativeLoaded) {
			try {
				AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
					System.loadLibrary("j9criu29"); //$NON-NLS-1$
					nativeLoaded = true;
					return null;
				});
			} catch (UnsatisfiedLinkError e) {
				if (System.getProperty("enable.j9internal.checkpoint.hook.api.debug") != null) { //$NON-NLS-1$
					e.printStackTrace();
				}
			}
		}

		return nativeLoaded;
	}

	/**
	 * Sets the directory that will hold the images upon checkpoint. This must be
	 * set before calling {@link #checkpointJVM()}.
	 *
	 * @param imageDir the directory as a java.nio.file.Path
	 * @return this
	 * @throws NullPointerException     if imageDir is null
	 * @throws SecurityException        if no permission to access imageDir
	 * @throws IllegalArgumentException if imageDir is not a valid directory
	 */
	public CRIUSupport setImageDir(Path imageDir) {
		Objects.requireNonNull(imageDir, "Image directory cannot be null");
		if (!Files.isDirectory(imageDir)) {
			throw new IllegalArgumentException("imageDir is not a valid directory");
		}
		String dir = imageDir.toAbsolutePath().toString();

		/* [IF JAVA_SPEC_VERSION < 17] */
		@SuppressWarnings({"deprecation" })
		/* [ENDIF] JAVA_SPEC_VERSION < 17 */
		SecurityManager manager = System.getSecurityManager();
		if (manager != null) {
			manager.checkWrite(dir);
		}

		this.imageDir = dir;
		return this;
	}

	/**
	 * Controls whether process trees are left running after checkpoint.
	 * <p>
	 * Default: false
	 *
	 * @param leaveRunning
	 * @return this
	 */
	public CRIUSupport setLeaveRunning(boolean leaveRunning) {
		this.leaveRunning = leaveRunning;
		return this;
	}

	/**
	 * Controls ability to dump shell jobs.
	 * <p>
	 * Default: false
	 *
	 * @param shellJob
	 * @return this
	 */
	public CRIUSupport setShellJob(boolean shellJob) {
		this.shellJob = shellJob;
		return this;
	}

	/**
	 * Controls whether to dump only one end of a unix socket pair.
	 * <p>
	 * Default: false
	 *
	 * @param extUnixSupport
	 * @return this
	 */
	public CRIUSupport setExtUnixSupport(boolean extUnixSupport) {
		this.extUnixSupport = extUnixSupport;
		return this;
	}

	/**
	 * Sets the verbosity of log output. Available levels:
	 * <ol>
	 * <li>Only errors
	 * <li>Errors and warnings
	 * <li>Above + information messages and timestamps
	 * <li>Above + debug
	 * </ol>
	 * <p>
	 * Default: 2
	 *
	 * @param logLevel verbosity from 1 to 4 inclusive
	 * @return this
	 * @throws IllegalArgumentException if logLevel is not valid
	 */
	public CRIUSupport setLogLevel(int logLevel) {
		if (logLevel > 0 && logLevel <= 4) {
			this.logLevel = logLevel;
		} else {
			throw new IllegalArgumentException("Log level must be 1 to 4 inclusive");
		}
		return this;
	}

	/**
	 * Write log output to logFile.
	 * <p>
	 * Default: criu.log
	 *
	 * @param logFile name of the file to write log output to. The path to the file
	 *                can be set with {@link #setWorkDir(Path)}.
	 * @return this
	 * @throws IllegalArgumentException if logFile is null or a path
	 */
	public CRIUSupport setLogFile(String logFile) {
		if (logFile != null && !logFile.contains(File.separator)) {
			this.logFile = logFile;
		} else {
			throw new IllegalArgumentException("Log file must not be null and not be a path");
		}
		return this;
	}

	/**
	 * Controls whether to dump file locks.
	 * <p>
	 * Default: false
	 *
	 * @param fileLocks
	 * @return this
	 */
	public CRIUSupport setFileLocks(boolean fileLocks) {
		this.fileLocks = fileLocks;
		return this;
	}

	/**
	 * Controls whether to re-establish TCP connects.
	 * <p>
	 * Default: false
	 *
	 * @param tcpEstablished
	 * @return this
	 */
	public CRIUSupport setTCPEstablished(boolean tcpEstablished) {
		this.tcpEstablished = tcpEstablished;
		return this;
	}

	/**
	 * Controls whether auto dedup of memory pages is enabled.
	 * <p>
	 * Default: false
	 *
	 * @param autoDedup
	 * @return this
	 */
	public CRIUSupport setAutoDedup(boolean autoDedup) {
		this.autoDedup = autoDedup;
		return this;
	}

	/**
	 * Controls whether memory tracking is enabled.
	 * <p>
	 * Default: false
	 *
	 * @param trackMemory
	 * @return this
	 */
	public CRIUSupport setTrackMemory(boolean trackMemory) {
		this.trackMemory = trackMemory;
		return this;
	}

	/**
	 * Sets the directory where non-image files are stored (e.g. logs).
	 * <p>
	 * Default: same as path set by {@link #setImageDir(Path)}.
	 *
	 * @param workDir the directory as a java.nio.file.Path
	 * @return this
	 * @throws NullPointerException     if workDir is null
	 * @throws SecurityException        if no permission to access workDir
	 * @throws IllegalArgumentException if workDir is not a valid directory
	 */
	public CRIUSupport setWorkDir(Path workDir) {
		Objects.requireNonNull(workDir, "Work directory cannot be null");
		if (!Files.isDirectory(workDir)) {
			throw new IllegalArgumentException("workDir is not a valid directory");
		}
		String dir = workDir.toAbsolutePath().toString();

		/* [IF JAVA_SPEC_VERSION < 17] */
		@SuppressWarnings({"deprecation" })
		/* [ENDIF] JAVA_SPEC_VERSION < 17 */
		SecurityManager manager = System.getSecurityManager();
		if (manager != null) {
			manager.checkWrite(dir);
		}

		this.workDir = dir;
		return this;
	}

	/**
	 * Append new environment variables to the set returned by ProcessEnvironment.getenv(...) upon
	 * restore. All pre-existing (environment variables from checkpoint run) env
	 * vars are retained. All environment variables specified in the envFile are
	 * added as long as they do not modifiy pre-existeing environment variables.
	 *
	 * Format for envFile is the following: ENV_VAR_NAME1=ENV_VAR_VALUE1 ...
	 * ENV_VAR_NAMEN=ENV_VAR_VALUEN
	 *
	 * @param envFile The file that contains the new environment variables to be
	 *                added
	 * @return this
	 */
	public CRIUSupport registerRestoreEnvFile(Path envFile) {
		this.envFile = envFile;
		return this;
	}

	/**
	 * User hook that is run before checkpointing the JVM.
	 *
	 * Hooks will be run in single threaded mode, no other application threads
	 * will be active. Users should avoid synchronization of objects that are not owned
	 * by the thread, terminally blocking operations and launching new threads in the hook.
	 *
	 * @param hook user hook
	 *
	 * @return this
	 *
	 * TODO: Additional JVM capabilities will be added to prevent certain deadlock scenarios
	 */
	public CRIUSupport registerPostRestoreHook(Runnable hook) {
		if (hook != null) {
			J9InternalCheckpointHookAPI.registerPostRestoreHook(USER_HOOKS_PRIORITY, "User post-restore hook", ()->{ //$NON-NLS-1$
				try {
					hook.run();
				} catch (Throwable t) {
					throw new RestoreException("Exception thrown when running user post-restore hook", 0, t);
				}
			});
		}
		return this;
	}

	/**
	 * User hook that is run after restoring a checkpoint image.
	 *
	 * Hooks will be run in single threaded mode, no other application threads
	 * will be active. Users should avoid synchronization of objects that are not owned
	 * by the thread, terminally blocking operations and launching new threads in the hook.
	 *
	 * @param hook user hook
	 *
	 * @return this
	 *
	 * TODO: Additional JVM capabilities will be added to prevent certain deadlock scenarios
	 */
	public CRIUSupport registerPreSnapshotHook(Runnable hook) {
		if (hook != null) {
			J9InternalCheckpointHookAPI.registerPreCheckpointHook(USER_HOOKS_PRIORITY, "User pre-checkpoint hook", ()->{ //$NON-NLS-1$
				try {
					hook.run();
				} catch (Throwable t) {
					throw new JVMCheckpointException("Exception thrown when running user pre-checkpoint hook", 0, t);
				}
			});
		}
		return this;
	}

	@SuppressWarnings("restriction")
	private void registerRestoreEnvVariables() {
		if (this.envFile == null) {
			return;
		}

		J9InternalCheckpointHookAPI.registerPostRestoreHook(RESTORE_ENVIRONMENT_VARIABLES_PRIORITY,
				"Restore environment variables via env file: " + envFile, () -> { //$NON-NLS-1$
					if (!Files.exists(this.envFile)) {
						throw throwSetEnvException(new IllegalArgumentException(
								"Restore environment variable file " + envFile + " does not exist."));
					}

					String file = envFile.toAbsolutePath().toString();

					try (BufferedReader envFileReader = new BufferedReader(new FileReader(file))) {

						Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment"); //$NON-NLS-1$
						Class<?> stringEnvironmentClass = Class.forName("java.lang.ProcessEnvironment$StringEnvironment"); //$NON-NLS-1$
						Class<?> variableClass = Class.forName("java.lang.ProcessEnvironment$Variable"); //$NON-NLS-1$
						Class<?> valueClass = Class.forName("java.lang.ProcessEnvironment$Value"); //$NON-NLS-1$
						Field theUnmodifiableEnvironmentHandle = processEnvironmentClass.getDeclaredField("theUnmodifiableEnvironment"); //$NON-NLS-1$
						Field theEnvironmentHandle = processEnvironmentClass.getDeclaredField("theEnvironment"); //$NON-NLS-1$
						Constructor<?> newStringEnvironment = stringEnvironmentClass.getConstructor(new Class<?>[] { Map.class });
						Method variableValueOf = variableClass.getDeclaredMethod("valueOf", new Class<?>[] { String.class }); //$NON-NLS-1$
						Method valueValueOf = valueClass.getDeclaredMethod("valueOf", new Class<?>[] { String.class }); //$NON-NLS-1$
						theUnmodifiableEnvironmentHandle.setAccessible(true);
						theEnvironmentHandle.setAccessible(true);
						newStringEnvironment.setAccessible(true);
						variableValueOf.setAccessible(true);
						valueValueOf.setAccessible(true);

						@SuppressWarnings("unchecked")
						Map<String, String> oldTheUnmodifiableEnvironment = (Map<String, String>) theUnmodifiableEnvironmentHandle
								.get(processEnvironmentClass);
						@SuppressWarnings("unchecked")
						Map<Object, Object> theEnvironment = (Map<Object, Object>) theEnvironmentHandle
								.get(processEnvironmentClass);

						String entry = null;

						List<String> illegalKeys = new ArrayList<>(0);
						while ((entry = envFileReader.readLine()) != null) {
							if (!entry.isBlank()) {
								// Only split into 2 (max) allow "=" to be contained in the value.
								String entrySplit[] = entry.split("=", 2); //$NON-NLS-1$
								if (entrySplit.length != 2) {
									throw new IllegalArgumentException(
											"Env File entry is not in the correct format: [envVarName]=[envVarVal]: "
													+ entry);
								}

								String name = entrySplit[0];
								String newValue = entrySplit[1];
								String oldValue = oldTheUnmodifiableEnvironment.get(name);
								if (oldValue != null) {
									if (!Objects.equals(oldValue, newValue)) {
										illegalKeys.add(name);
									}
								} else {
									theEnvironment.put(variableValueOf.invoke(null, name), valueValueOf.invoke(null, newValue));
								}
							}
						}

						if (!illegalKeys.isEmpty()) {
							throw new IllegalArgumentException(String.format("Env file entry cannot modifiy pre-existing environment keys: %s", String.valueOf(illegalKeys)));
						}

						@SuppressWarnings("unchecked")
						Map<String, String> newTheUnmodifiableEnvironment = (Map<String, String>) newStringEnvironment.newInstance(theEnvironment);

						unsafe.putObject(processEnvironmentClass, unsafe.staticFieldOffset(theUnmodifiableEnvironmentHandle),
								Collections.unmodifiableMap(newTheUnmodifiableEnvironment));

					} catch (Throwable t) {
						throw throwSetEnvException(t);
					}
				});
	}

	private static RestoreException throwSetEnvException(Throwable cause) {
		throw new RestoreException("Failed to setup new environment variables", 0, cause);
	}

	/**
	 * Checkpoint the JVM. This operation will use the CRIU options set by the
	 * options setters.
	 *
	 * @throws UnsupportedOperationException if CRIU is not supported
	 * @throws JVMCheckpointException        if a JVM error occurred before
	 *                                       checkpoint
	 * @throws SystemCheckpointException     if a CRIU operation failed
	 * @throws RestoreException              if an error occurred during or after
	 *                                       restore
	 */
	public void checkpointJVM() {
		/* Add env variables restore hook */
		registerRestoreEnvVariables();

		if (isCRIUSupportEnabled()) {
			checkpointJVMImpl(imageDir, leaveRunning, shellJob, extUnixSupport, logLevel, logFile, fileLocks, workDir,
					tcpEstablished, autoDedup, trackMemory);
		} else {
			throw new UnsupportedOperationException("CRIU support is not enabled");
		}
	}
}
