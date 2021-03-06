/*
 * Copyright (c) 2017. Fengguo Wei and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Detailed contributors are listed in the CONTRIBUTOR.md
 */

package hu.ssh.progressbar.console;

import hu.ssh.progressbar.AbstractProgressBar;
import hu.ssh.progressbar.Progress;
import hu.ssh.progressbar.console.replacers.*;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

/**
 * A console progress bar.
 * <p>
 * It can display a human readable progress bar with the specified format on the selected
 * output.
 * <p>
 * Here is a basic usage:
 *
 * <pre>
 * ProgressBar progressBar = ConsoleProgressBar.on(System.out);
 * progressBar.tick(20);
 * progressBar.finish();
 * </pre>
 *
 * It can be configured with custom format:
 *
 * <pre>
 * ProgressBar progressBar = ConsoleProgressBar.on(System.out)
 * 		.withFormat(&quot;[:bar]&quot;);
 * </pre>
 *
 * Or can be set to use custom number of steps instead of the default <code>100</code>:
 *
 * <pre>
 * ProgressBar progressBar = ConsoleProgressBar.on(System.out)
 * 		.withTotalSteps(25);
 * </pre>
 *
 * @author KARASZI István (github@spam.raszi.hu)
 */
public final class ConsoleProgressBar extends AbstractProgressBar {
	private static final char LINE_FEED = '\n';
	public static final char CARRIAGE_RETURN = '\r';

	private static final long DEFAULT_STEPS = 100;
	private static final String DEFAULT_FORMAT = "[:bar] :percent% :eta";
	private static final int DEFAULT_PROGRESSBAR_WIDTH = 50;
	private static final Set<Replacer> DEFAULT_REPLACERS = getDefaultReplacers(DEFAULT_PROGRESSBAR_WIDTH);

	private final Set<Replacer> replacers;
	private final PrintStream streamToUse;
	private final String outputFormat;

	private int previousLength = 0;

	private ConsoleProgressBar(
			final PrintStream streamToUse,
			final long totalSteps,
			final String progressBarFormat,
			final Set<Replacer> replacers)
	{
		super(totalSteps);

		this.replacers = replacers;
		this.streamToUse = streamToUse;
		this.outputFormat = progressBarFormat;
	}

	/**
	 * A console progress bar on the selected PrintStream.
	 *
	 * @param streamToUse
	 *            the PrintStream on the ProgressBar should appear
	 * @return a progress bar with the desired configuration
	 */
	public static ConsoleProgressBar on(final PrintStream streamToUse) {
		Preconditions.checkNotNull(streamToUse);

		return new ConsoleProgressBar(streamToUse,
				DEFAULT_STEPS,
				DEFAULT_FORMAT,
				DEFAULT_REPLACERS);
	}

	/**
	 * Changes the output format of the actual ProgressBar.
	 *
	 * @param outputFormat
	 *            the new output format
	 * @return a progress bar with the desired configuration
	 */
	public ConsoleProgressBar withFormat(final String outputFormat) {
		Preconditions.checkNotNull(outputFormat);

		return new ConsoleProgressBar(streamToUse,
				totalSteps,
				outputFormat,
				replacers);
	}

	@Override
	public ConsoleProgressBar withTotalSteps(final int totalSteps) {
		Preconditions.checkArgument(totalSteps != 0);

		return new ConsoleProgressBar(streamToUse,
				totalSteps,
				outputFormat,
				replacers);
	}

	/**
	 * Changes the replacers for the actual ProgressBar.
	 *
	 * @param replacers
	 *            the new replacers to use
	 * @return a progress bar with the desired configuration
	 */
	public ConsoleProgressBar withReplacers(final Collection<Replacer> replacers) {
		Preconditions.checkNotNull(replacers);

		return new ConsoleProgressBar(streamToUse,
				totalSteps,
				outputFormat,
				ImmutableSet.copyOf(replacers));
	}

	/**
	 * Gets the default replacers.
	 *
	 * @param progressBarWidth
	 *            the width of the progress bar
	 * @return the configured replacers
	 */
	private static Set<Replacer> getDefaultReplacers(final int progressBarWidth) {
		return ImmutableSet.of(
				new BarReplacer(progressBarWidth),
				new PercentageReplacer(),
				new RemainingTimeReplacer(),
				new ElapsedTimeReplacer(),
				new TotalTimeReplacer(),
				new RemainingTaskReplacer()
				);
	}

	@Override
	protected void updateProgressBar(final Progress progress) {
		final String actualBar = getActualProgressBar(progress);
		streamToUse.print(CARRIAGE_RETURN + actualBar + getGarbageCleaning(actualBar.length()));
		streamToUse.flush();
	}

	@Override
	protected void finishProgressBar() {
		streamToUse.print(LINE_FEED);
	}

	private String getActualProgressBar(final Progress progress) {
		String bar = outputFormat;

		for (final Replacer replacer : replacers) {
			final String identifier = replacer.getReplaceIdentifier();

			if (!bar.contains(identifier)) {
				continue;
			}

			bar = bar.replace(identifier, replacer.getReplacementForProgress(progress));
		}

		return bar;
	}

	private String getGarbageCleaning(final int actualLength) {
		if (actualLength >= previousLength) {
			previousLength = actualLength;
			return "";
		}

		final String garbageCleaner = Strings.repeat(" ", previousLength - actualLength);
		previousLength = actualLength;

		return garbageCleaner;
	}
}