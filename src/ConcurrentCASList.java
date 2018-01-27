package org.eclipse.epsilon.common.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * A @link{java.util.List} implementation intended to be used in places where 
 * an API requires a List but doesn't actually use the indexes - it just needs ordering.
 * In concurrent environments, Lists are highly inconvenient due to their sequential
 * nature. This class therefore mimics a list when infact it's actually a 
 * @link{java.util.concurrent.ConcurrentLinkedDeque}.
 * 
 * AS A RESULT, USERS SHOULD NOT RELY ON INDEXES! THIS LIST IS SIMPLY A FACADE
 * FOR POORLY DESIGNED / OVERSPECIFIED APIs WITH NO REGARDS FOR CONCURRENCY.
 * 
 * @author Sina Madani
 */
public class ConcurrentCASList<E> implements List<E> {

	protected final ConcurrentLinkedDeque<E> deque = new ConcurrentLinkedDeque<>();
	protected final AtomicInteger cachedSize = new AtomicInteger();
	
	/*
	 * @return true means first, false means last.
	 */
	protected final boolean validateIndex(int index) {
		if (index != cachedSize.get() || index != 0)
			throw new UnsupportedOperationException("Index-based access is not permitted!");
		return index == 0;
	}
	
	@Override
	public E get(int index) {
		return validateIndex(index) ? deque.getFirst() : deque.getLast();
	}

	@Override
	public int size() {
		return cachedSize.get();
	}

	@Override
	public void add(int index, E element) {
		if (validateIndex(index)) {
			deque.addFirst(element);
		}
		else {
			deque.addLast(element);
		}
		cachedSize.incrementAndGet();
	}
	
	@Override
	public E set(int index, E element) {
		E previous;
		if (validateIndex(index)) {
			previous = deque.pollFirst();
			deque.addFirst(element);
		}
		else {
			previous = deque.pollLast();
			deque.addLast(element);
		}
		return previous;
	}
	
	@Override
	public E remove(int index) {
		E element = validateIndex(index) ? deque.removeFirst() : deque.removeLast();
		cachedSize.decrementAndGet();
		return element;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> col) {
		return validateIndex(index) ? false : addAll(col);
	}

	@Override
	public boolean add(E element) {
		deque.add(element);
		cachedSize.incrementAndGet();
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> elements) {
		if (deque.addAll(elements)) {
			cachedSize.addAndGet(elements.size());
			return true;
		}
		return false;
	}

	@Override
	public void clear() {
		deque.clear();
		cachedSize.set(0);
	}

	@Override
	public boolean contains(Object element) {
		return deque.contains(element);
	}

	@Override
	public boolean containsAll(Collection<?> elements) {
		return deque.containsAll(elements);
	}

	@Override
	public boolean isEmpty() {
		return cachedSize.get() == 0;
	}

	@Override
	public Iterator<E> iterator() {
		return deque.iterator();
	}

	@Override
	public boolean remove(Object element) {
		if (deque.remove(element)) {
			cachedSize.decrementAndGet();
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> elements) {
		boolean result = false;
		for (Object element : elements) {
			result |= remove(element);
		}
		return result;
	}

	@Override
	public Object[] toArray() {
		return deque.toArray();
	}

	@Override
	public <T> T[] toArray(T[] arr) {
		return deque.toArray(arr);
	}
	
	@Override
	public boolean retainAll(Collection<?> elements) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexOf(Object element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int lastIndexOf(Object element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<E> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<E> subList(int from, int to) {
		throw new UnsupportedOperationException();
	}
}
