package edu.brown.library.vitro.auth.policy;

import edu.cornell.mannlib.vitro.webapp.auth.identifier.IdentifierBundle;
import edu.cornell.mannlib.vitro.webapp.auth.policy.BasicPolicyDecision;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ServletPolicyList;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ifaces.Authorization;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ifaces.PolicyDecision;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ifaces.PolicyIface;
import edu.cornell.mannlib.vitro.webapp.auth.policy.specialrelationships.AbstractRelationshipPolicy;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.Lock;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.display.DisplayObjectProperty;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.display.DisplayObjectPropertyStatement;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.propstmt.AddObjectPropertyStatement;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.publish.PublishObjectPropertyStatement;
import edu.cornell.mannlib.vitro.webapp.dao.jena.QueryUtils;
import edu.cornell.mannlib.vitro.webapp.servlet.setup.JenaDataSourceSetupBase;

import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.RequestedAction;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.propstmt.AbstractPropertyStatementAction;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.propstmt.EditObjectPropertyStatement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static edu.cornell.mannlib.vitro.webapp.auth.requestedAction.RequestedAction.SOME_LITERAL;
import static edu.cornell.mannlib.vitro.webapp.auth.requestedAction.RequestedAction.SOME_PREDICATE;
import static edu.cornell.mannlib.vitro.webapp.auth.requestedAction.RequestedAction.SOME_URI;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Collections;
import java.util.List;

/**
 * Created by ted on 3/12/15.
 */
public class HiddenObjectPropertyPolicy implements PolicyIface{
    private static final Log log = LogFactory
            .getLog(HiddenObjectPropertyPolicy.class);

    private final Dataset dataset;
    private static final Syntax SYNTAX = Syntax.syntaxARQ;

    public HiddenObjectPropertyPolicy(ServletContext ctx) {
        this.dataset = JenaDataSourceSetupBase.getStartupDataset(ctx);
    }

    @Override
    public PolicyDecision isAuthorized(IdentifierBundle whoToAuth,
                                       RequestedAction whatToAuth) {
        String objURI = null;
        if (whatToAuth instanceof DisplayObjectPropertyStatement) {
            objURI = ((DisplayObjectPropertyStatement) whatToAuth).getObjectUri();
        } else if (whatToAuth instanceof PublishObjectPropertyStatement) {
            objURI = ((PublishObjectPropertyStatement) whatToAuth).getObjectUri();
        }
        else {
            return inconclusiveDecision("Not applicable");
        }
        if ((objURI == null)) {
            return inconclusiveDecision("Not applicable");
        }
        if (relatedToHiddenClass(objURI)) {
            log.debug("Not authorizing object prop view.");
            return unauthorizedDecision("Related to hidden property");
        } else {
            log.debug("OK to view object property.");
        }

        return inconclusiveDecision("whatToAuth was null");
    }

    private static final String QUERY_TEMPLATE = "" + //
            "ASK {" + //
            "     ?uri a <http://vivo.brown.edu/ontology/display#Hidden> ." + //
            "} ";

    private Boolean relatedToHiddenClass(String uri) {
        String rq = assembleQueryString(uri);
        Boolean askResult = executeQuery(rq);
        return askResult;
    }

    private String assembleQueryString(String uri) {
        String q = QUERY_TEMPLATE;
        q = QueryUtils.subUriForQueryVar(q, "uri", uri);
        return q;
    }

    private Boolean executeQuery(String queryStr) {
        log.debug("select query is: '" + queryStr + "'");
        QueryExecution qe = null;
        dataset.getLock().enterCriticalSection(Lock.READ);
        try {
            Query query = QueryFactory.create(queryStr, SYNTAX);
            qe = QueryExecutionFactory.create(query, dataset);
            return qe.execAsk();
        } catch (Exception e) {
            log.error("Failed to execute the ASK query: " + queryStr, e);
            return false;
        } finally {
            if (qe != null) {
                qe.close();
            }
            dataset.getLock().leaveCriticalSection();
        }
    }

    private PolicyDecision inconclusiveDecision(String message) {
        return new BasicPolicyDecision(Authorization.INCONCLUSIVE, getClass()
                .getSimpleName() + ": " + message);
    }

    private PolicyDecision unauthorizedDecision(String message) {
        return new BasicPolicyDecision(Authorization.UNAUTHORIZED, getClass()
                .getSimpleName() + ": " + message);
    }

    public static class Setup implements ServletContextListener {
        @Override
        public void contextInitialized(ServletContextEvent sce) {
            ServletContext ctx = sce.getServletContext();

            ServletPolicyList.addPolicyAtFront(ctx, new HiddenObjectPropertyPolicy(
                    ctx));
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) { /* nothing */
        }
    }

}
