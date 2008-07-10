package orc.orchard.api;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import orc.orchard.JobConfiguration;
import orc.orchard.JobEvent;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.QuotaException;
import orc.orchard.errors.UnsupportedFeatureException;
import orc.orchard.oil.Oil;


/**
 * Broker used to create and manage running jobs.
 * 
 * <p>
 * Originally the executor only allow clients to create jobs and clients had to
 * use a separate service to manage running jobs. This created a lot of extra
 * work for both the client and the server for marginal benefit, so I combined
 * the services. Some examples of the "extra work" (mostly caused by the Java
 * web services and servlet environments):
 * <ul>
 * <li>Clients must support dynamically-generated web service proxies. (This is
 * really the killer issue, since some platforms like Java make this hard).
 * <li>It's awkward to pass information between the executor and job services.
 * It requires an out-of-band channel.
 * <li>It requires context to be encoded in the URL, which may not be practical
 * for some services. In RPC-style services it's best if all context is passed
 * explicitly as arguments.
 * <li>Building job URLs requires knowledge about the protocol, so it creates
 * more work to build a new protocol front-end.
 * <li>
 * </ul>
 * 
 * <p>
 * The lifecycle of a job:
 * 
 * <ol>
 * <li>Client calls compile to get a job ID.
 * <li>Client calls jobStart to start the job.
 * <li>In a loop,
 * <ol>
 * <li>Client calls jobEvents to get publications.
 * <li>Client calls purgeJobEvents to clear the publication buffer.
 * </ol>
 * <li>Client may call haltJob to force the job to end.
 * <li>Job finishes.
 * <li>Client calls finishJob to clean up the job.
 * </ol>
 * 
 * <p>
 * Note that the job publication buffer has a fixed size, so if you don't call
 * purgeJobEvents regularly your job will be suspended when the buffer fills.
 * 
 * @author quark
 */
public interface ExecutorServiceInterface extends Remote {
	/**
	 * Register a new job for execution, using the provided job configuration.
	 * 
	 * @return String Job ID of new job.
	 * @throws QuotaException
	 *             if registering this job would exceed quotas.
	 * @throws InvalidOilException
	 *             if the program is invalid.
	 * @throws UnsupportedFeatureException
	 *             if the executor does not support some part of the
	 *             configuration.
	 */
	public String submitConfigured(String devKey, Oil program, JobConfiguration configuration) throws QuotaException,
			InvalidOilException, UnsupportedFeatureException, RemoteException;
	/**
	 * Register a new job for execution, using a default JobConfiguration.
	 */
	public String submit(String devKey, Oil program) throws QuotaException, InvalidOilException, RemoteException;
	/**
	 * Combine compilation and submission into a single step.
	 * This is useful for simple clients that don't want to
	 * bother calling a separate compiler.
	 */
	public String compileAndSubmit(String devKey, String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException;
	/**
	 * Combine compilation and submission into a single step.
	 */
	public String compileAndSubmitConfigured(String devKey, String program, JobConfiguration configuration) throws QuotaException, InvalidProgramException, InvalidOilException, UnsupportedFeatureException, RemoteException;
	/**
	 * URIs of unfinished jobs started from this executor.
	 */
	public Set<String> jobs(String devKey) throws RemoteException;
	/**
	 * Begin executing the job.
	 * 
	 * @throws InvalidJobStateException
	 *             if the job was already started, or was aborted.
	 */
	public void startJob(String devKey, String job) throws InvalidJobStateException, RemoteException;
	/**
	 * Indicate that the client is done with the job. The job will be halted if
	 * necessary.
	 * 
	 * <p>
	 * Once this method is called, the service provider is free to garbage
	 * collect the service and the service URL may become invalid, so no other
	 * methods should be called after this.
	 * 
	 * @throws InvalidJobStateException
	 *             if the job is RUNNING or WAITING.
	 * @throws RemoteException
	 */
	public void finishJob(String devKey, String job) throws InvalidJobStateException, RemoteException;
	/**
	 * Halt the job safely, using the same termination semantics as the "pull"
	 * combinator.
	 */
	public void haltJob(String devKey, String job) throws RemoteException;
	/**
	 * What is the job's state? Possible return values:
	 * NEW: not yet started.
	 * RUNNING: started and processing tokens.
	 * WAITING: started and waiting for response from a site.
	 * DONE: finished executing. 
	 * @return the current state of the job.
	 */
	public String jobState(String devKey, String job) throws RemoteException;
	/**
	 * Retrieve events. If no events occurred, block until at least one occurs.
	 * If the job finishes without any more events happening, an empty list will
	 * be returned.
	 * 
	 * @throws InterruptedException
	 *             if the request times out.
	 */
	public List<JobEvent> jobEvents(String devKey, String job) throws RemoteException, InterruptedException;
	/**
	 * Purge all events from the event buffer with sequence number less than or
	 * equal to the argument. The client is responsible for calling this method
	 * regularly to keep the event buffer from filling up.
	 * 
	 * @throws RemoteException
	 */
	public void purgeJobEvents(String devKey, String job, int sequence) throws RemoteException;
}