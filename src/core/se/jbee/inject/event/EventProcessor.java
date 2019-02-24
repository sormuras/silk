package se.jbee.inject.event;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

import se.jbee.inject.container.Initialiser;

/**
 * The {@link EventProcessor} is the unit that functionally connects the
 * generated event handler interface proxies with the registered implementations
 * (handlers) for each event type.
 * 
 * When a handler interface should be injected the injection is wired to the
 * {@link #getProxy(Class)} method that provides a proxy implementation that
 * when called creates a {@link Event} and calls {@link #dispatch(Event)},
 * {@link #compute(Event)} or {@link #computeEventually(Event)} depending on the
 * signature of the called proxy method.
 * 
 * This allows for all kind of scenarios. The simplest being a 1:n listener
 * notification. More advance scenarios do concurrency control and load
 * distribution for computations and make parallel computation very convenient
 * for the caller while providing clear semantics for the concurrent processing.
 * The {@link EventPreferences} generated by the {@link EventReflector} can be
 * used to control different aspects of the processing for each event type
 * individually.
 * 
 * The additional methods {@link #deregister(Class, Object)} and
 * {@link #await(Class)} exist to allow to build a utility layer on top of the
 * {@link EventProcessor}. For example to do a blocking wait or manual
 * management of handlers.
 * 
 * Usually handler are never deregistered. Instead the {@link EventProcessor}
 * uses {@link java.lang.ref.WeakReference}s so that handlers automatically are
 * "deregistered" if they no longer are required by any other part of the
 * application.
 */
public interface EventProcessor extends AutoCloseable {

	/**
	 * This will cause the calling thread to wait until there is a implementation
	 * for the given event.
	 *
	 * Instead of support blocking in case no implementation is known for a event
	 * the API allows to call this method explicitly to wait until a implementation
	 * is registered and be notified. This allows to build any kind of custom
	 * blocking wait logic around the {@link EventProcessor}.
	 * 
	 * @param event type of event handler to wait for.
	 */
	<E> void await(Class<E> event) throws InterruptedException;
	
	/**
	 * Registers a event handler implementation to be used by this
	 * {@link EventProcessor}.
	 * 
	 * The processor uses all registered handlers to {@link #dispatch(Event)} events
	 * to them or just a single one to {@link #compute(Event)} a result depending on
	 * the handler method signature and the {@link EventPreferences} given with an
	 * {@link Event}.
	 * 
	 * When a event type is {@link EventModule#handle(Class)}d the registration
	 * occurs "automatically" though {@link Initialiser}s for the event handler
	 * interface. For handler implementations it is sufficient to implement that
	 * handler interface. No explicit connection needs to be bound.
	 * 
	 * @param event type of events handled by the given handler (essentially the
	 *            common interface of all handlers for the event type)
	 * @param handler the implementation handling the events
	 */
	<E> void register(Class<E> event, E handler);

	/**
	 * Explicitly remove a previously {@link #register(Class, Object)}ed handler
	 * from this {@link EventProcessor}.
	 * 
	 * This method exist do build further utilities based on the
	 * {@link EventProcessor} but is not used by the standard event handling setup
	 * created by {@link EventModule#handle(Class)} as no explicit connection is
	 * made between the handler implementation and the interface. Instead the
	 * {@link EventProcessor} uses {@link java.lang.ref.WeakReference}s to the
	 * handler implementation so that those can be garbage collected as if they
	 * aren't linked to the event system.
	 * 
	 * @param event type of events handled by the given handler
	 * @param handler the handler implementation to deregister.
	 */
	<E> void deregister(Class<E> event, E handler);
	
	/**
	 * Called when a handler interface should be injected. This
	 * {@link EventProcessor} creates a proxy implementation of the given event
	 * interface that forwards calls to any of the interface's methods to one of the
	 * event processing methods: {@link #dispatch(Event)}, {@link #compute(Event)}
	 * or {@link #computeEventually(Event)} depending on the method signature and
	 * {@link EventPreferences}.
	 * 
	 * For the user/caller it becomes transparent that a method call of the event
	 * interface is not directly processed by a implementation but handled by this
	 * {@link EventProcessor} that uses registered implementations to perform the
	 * computation.
	 * 
	 * The injection of proxies generated by a {@link EventProcessor} is
	 * "automatically" wired for the user when a event type is registered using
	 * {@link EventModule#handle(Class)}.
	 * 
	 * @param event type of events (the interface used to describe possible events)
	 * @return A proxy for the given event type feeding calls to its methods to this
	 *         {@link EventProcessor}.
	 */
	<E> E getProxy(Class<E> event);

	/**
	 * Does a multi-dispatch (dispatch to all registered handlers) if
	 * {@link EventPreferences#isMultiDispatch()} is {@code true}, otherwise single
	 * dispatch if it is {@code false} using a round robin in case multiple handlers
	 * were registered.
	 * 
	 * The dispatch is usually asynchronous (method returns when event queued for
	 * processing) unless the {@link EventPreferences#isSyncMultiDispatch()} is set
	 * {@code true} in which case it synchronises at the end of the processing and
	 * returns when the event is processed.
	 * 
	 * Usually this method is called for methods which return nothing (void) but it
	 * can be used for any event handler method as long as the caller does not need
	 * the result.
	 * 
	 * When processed asynchronously this method will never throw any exception. The
	 * given exception cases only apply to synchronised processing.
	 * 
	 * @param event the event to dispatch
	 * @throws Throwable When synchronised the original exceptions thrown by the
	 *             implementing handler are thrown as long as the methods signature
	 *             permits it. This is {@link Throwable} as
	 *             {@link InvocationTargetException#getTargetException()} is a
	 *             {@link Throwable}.
	 * @throws EventException for problems related to the processing outside of the
	 *             handler method.
	 * @throws TimeoutException in case permitted by the handler method signature in
	 *             case of a timeout. Otherwise timeouts are wrapped in
	 *             {@link EventException}s.
	 * @throws RejectedExecutionException in case the processor does not accept the
	 *             event for some reason.
	 */
	<E> void dispatch(Event<E, ?> event) throws Throwable;
	
	/**
	 * Forwards the handler method call to a registered event handler
	 * implementation. If multiple handlers were registered usually a round robin is
	 * used to distribute load.
	 * 
	 * For boolean results the {@link EventPreferences#isAggregatedMultiDispatch()}
	 * can be set to do a multi-dispatch to all registered handlers. The result is
	 * {@code true} if *any* of the handlers returned true, otherwise it is
	 * {@code false}.
	 * 
	 * @param event the event to compute a result for
	 * @return the result if the computation, usually this is the result returned by
	 *         the {@link #register(Class, Object)}ed handler implementation called
	 *         by this {@link EventProcessor}.
	 * @throws Throwable A exceptions originally thrown by the implementing handler
	 *             (as long as the methods signature permits it). This is
	 *             {@link Throwable} as
	 *             {@link InvocationTargetException#getTargetException()} is a
	 *             {@link Throwable}.
	 * @throws EventException for problems related to the processing outside of the
	 *             handler method.
	 * @throws TimeoutException in case permitted by the handler method signature in
	 *             case of a timeout. Otherwise timeouts are wrapped in
	 *             {@link EventException}s.
	 * @throws RejectedExecutionException in case the processor does not accept the
	 *             event for some reason.
	 */
	<E, T> T compute(Event<E, T> event) throws Throwable;
	
	<E, T extends Future<V>, V> Future<V> computeEventually(Event<E, T> event);
	
	/**
	 * The implementation is likely to use worker threads that are disposed at this
	 * point.
	 */
	@Override
	void close();
}
