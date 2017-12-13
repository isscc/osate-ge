package org.osate.ge.internal.util;

import org.osate.aadl2.ClassifierValue;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.ge.internal.query.Queryable;
import org.osate.ge.query.StandaloneQuery;
import org.osate.ge.services.QueryService;

public class PropertyValueUtil {
	private static StandaloneQuery classifierQuery = StandaloneQuery.create((rootQuery) -> rootQuery.children().children().filterByBusinessObjectCanonicalReference(qa -> qa).first());
	private static StandaloneQuery instanceObjectQuery = StandaloneQuery.create(
			(rootQuery) -> rootQuery.descendants().filterByBusinessObjectCanonicalReference(qa -> qa).first());

	public static Queryable getReferencedClassifier(final Queryable q,
			final ClassifierValue cv,
			final QueryService queryService) {
		// Decide whether to show it as connection or not.
		Queryable top = q;
		while(top.getParent() != null) {
			top = top.getParent();
		}

		return queryService.getFirstResult(classifierQuery, top, cv.getClassifier());
	}

	public static Queryable getReferencedInstanceObject(final Queryable q, final InstanceObject io,
			final QueryService queryService) {
		// Decide whether to show it as connection or not.
		Queryable top = q;
		while (top.getParent() != null) {
			top = top.getParent();
		}

		return queryService.getFirstResult(instanceObjectQuery, top, io);
	}
}
