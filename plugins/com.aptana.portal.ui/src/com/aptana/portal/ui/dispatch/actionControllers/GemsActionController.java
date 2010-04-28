/**
 * 
 */
package com.aptana.portal.ui.dispatch.actionControllers;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.mortbay.util.ajax.JSON;

import com.aptana.configurations.processor.ConfigurationStatus;
import com.aptana.configurations.processor.IConfigurationProcessor;
import com.aptana.portal.ui.dispatch.BrowserNotifier;
import com.aptana.portal.ui.dispatch.IBrowserNotificationConstants;

/**
 * A action controller for Ruby Gems related actions.<br>
 * 
 * @author Shalom Gibly <sgibly@aptana.com>
 */
public class GemsActionController extends AbstractActionController
{
	/**
	 * Starts a Job that computes the installed Gems in the system.<br>
	 * This Job will then notify any internal browser the result through the {@link BrowserNotifier}.
	 * 
	 * @return An immediate {@link IBrowserNotificationConstants#JSON_OK} to indicate that the Job was started, or an
	 *         error JSON notification if there is no configuration processor that can deal with this request.
	 */
	@ControllerAction
	public Object computeInstalledGems()
	{
		// Get the configuration processor (the method invocation in the AbstractActionController already checked that
		// it's valid)
		final IConfigurationProcessor processor = getProcessor();
		// Start a Job that will call the processor to compute the installed Gems and notify the browse when it's done
		Job computationJob = new Job(Messages.GemsActionController_computingGemsJobName)
		{
			protected IStatus run(IProgressMonitor monitor)
			{
				processor.addConfigurationProcessorListener(GemsActionController.this);
				processor.getStatus(monitor, null, true);
				processor.removeConfigurationProcessorListener(GemsActionController.this);
				return Status.OK_STATUS;
			}
		};
		computationJob.schedule();
		return IBrowserNotificationConstants.JSON_OK;
	}

	/**
	 * Returns a
	 * 
	 * @return
	 */
	@ControllerAction
	public Object getInstalledGems()
	{
		IConfigurationProcessor processor = getProcessor();
		// We rely on the fact that the ConfigurationStatus implements the JSON Convertible interface
		String processorResult = JSON.toString(processor.getStatus(null, null, false));
		// Construct a response which is similar to the notification structure being fired when
		// computeInstalledGems is invoked. This makes the JS side handling easier in both case.
		return BrowserNotifier.toJSONNotification(IBrowserNotificationConstants.EVENT,
				IBrowserNotificationConstants.EVENT_TYPE_RESPONSE, processorResult);
	}

	/**
	 * Listen to configuration status changes and notify the browser when needed.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void configurationStateChanged(ConfigurationStatus status, Set<String> attributesChanged)
	{
		// On the Gems controller, we only care status changes
		if (attributesChanged != null && attributesChanged.contains(ConfigurationStatus.STATUS))
		{
			String jsonStatus = JSON.toString(status);
			BrowserNotifier.getInstance().notifyBrowserInUIThread(Collections.EMPTY_LIST,
					IBrowserNotificationConstants.EVENT_ID_GEM_LIST, IBrowserNotificationConstants.EVENT_TYPE_CHANGED,
					jsonStatus);
		}
	}
}
