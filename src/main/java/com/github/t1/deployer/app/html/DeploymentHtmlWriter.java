package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.builder2.Button.*;
import static com.github.t1.deployer.app.html.builder2.ButtonGroup.*;
import static com.github.t1.deployer.app.html.builder2.Compound.*;
import static com.github.t1.deployer.app.html.builder2.DescriptionList.*;
import static com.github.t1.deployer.app.html.builder2.Form.*;
import static com.github.t1.deployer.app.html.builder2.Input.*;
import static com.github.t1.deployer.app.html.builder2.SizeVariation.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.StyleVariation.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import static com.github.t1.deployer.app.html.builder2.Tags.*;

import java.net.URI;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.*;
import com.github.t1.deployer.app.html.builder2.*;
import com.github.t1.deployer.app.html.builder2.DescriptionList.DescriptionListBuilder;
import com.github.t1.deployer.app.html.builder2.Form.FormBuilder;
import com.github.t1.deployer.app.html.builder2.Tags.AppendingComponent;
import com.github.t1.deployer.model.*;

@Provider
public class DeploymentHtmlWriter extends TextHtmlMessageBodyWriter<DeploymentResource> {
    private static final Compound AVAILABLE_VERSIONS = //
            compound( //
                    header(2).body(text("Available Versions")).build(), //
                    new Component() {
                        @Override
                        public void writeTo(BuildContext out) {
                            UriInfo uriInfo = out.get(UriInfo.class);
                            Version currentVersion = out.get(DeploymentResource.class).getVersion();
                            DescriptionListBuilder list = descriptionList().horizontal();
                            int i = 0;
                            for (Deployment deployment : out.get(DeploymentResource.class).getAvailableVersions()) {
                                if (i > 0)
                                    list.nl();
                                boolean isCurrent = deployment.getVersion().equals(currentVersion);
                                list //
                                .title(deployment.getVersion().getVersion()).style("padding-bottom: 10px") //
                                        .description(redeployButton("redeploy-" + i++, deployment, uriInfo, isCurrent)) //
                                        .build();
                            }
                            list.build().writeTo(out);
                        }

                        private Component redeployButton(String id, Deployment deployment, UriInfo uriInfo,
                                boolean isCurrent) {
                            FormBuilder form = form(id);
                            form.action(text(Deployments.path(uriInfo, deployment.getContextRoot())));
                            form.body(hiddenInput("contextRoot", deployment.getContextRoot().getValue()));
                            form.body(hiddenInput("checksum", deployment.getCheckSum().hexString()));
                            form.body(hiddenAction("redeploy"));

                            Static deployLabel = text(isCurrent ? "Redeploy" : "Deploy");
                            return compound( //
                                    form.build(), //
                                    buttonGroup().button( //
                                            button().size(XS).style(primary).forForm(id).body(deployLabel).build()) //
                                            .build() //
                            ).build();
                        }
                    }).build();

    private static final Component DEPLOYMENT_INFO = new Component() {
        @Override
        public void writeTo(BuildContext out) {
            DeploymentResource deployment = out.get(DeploymentResource.class);
            DescriptionListBuilder description = descriptionList().horizontal();

            description.title("Name").description(text(deployment.getName())).build();
            description.nl();
            description.title("Context-Root").description(text(deployment.getContextRoot())).build();
            description.nl();
            description.title("Version").description(textOr(deployment.getVersion(), "unknown")).build();
            description.nl();
            description.title("CheckSum").description(text(deployment.getCheckSum())).build();

            description.build().writeTo(out);
        }
    };

    private static final String MAIN_FORM_ID = "main";

    private static final AppendingComponent<URI> DEPLOYMENT_LINK = new AppendingComponent<URI>() {
        @Override
        protected URI contentFrom(BuildContext out) {
            return Deployments.path(out.get(UriInfo.class), out.get(DeploymentResource.class).getContextRoot());
        }
    };

    private static Component deployingActionComponent(String formId, String action, String title) {
        return div().body(compound( //
                form(formId).action(DEPLOYMENT_LINK) //
                        .body(hiddenInput().name("contextRoot").value(new AppendingComponent<ContextRoot>() {
                            @Override
                            protected ContextRoot contentFrom(BuildContext out) {
                                return out.get(DeploymentResource.class).getContextRoot();
                            }
                        }).build()) //
                        .body(hiddenInput().name("checksum").value(new AppendingComponent<CheckSum>() {
                            @Override
                            protected CheckSum contentFrom(BuildContext out) {
                                return out.get(DeploymentResource.class).getCheckSum();
                            }
                        }).build()) //
                        .body(hiddenAction(action)) //
                        .build(), //
                buttonGroup().button( //
                        button().size(S).style(danger).forForm(formId).body(text(title)).build() //
                        ).build() //
                ).build()).build();
    }

    private static final Compound EXISTING_DEPLOYMENT_FORM = compound("\n", //
            DEPLOYMENT_INFO, //
            deployingActionComponent("undeploy", "undeploy", "Undeploy"), //
            AVAILABLE_VERSIONS).build();

    private static final Compound NEW_DEPLOYMENT_FORM = compound( //
            tag("p").body(text("Enter the checksum of a new artifact to deploy")).build(), //
            form(MAIN_FORM_ID) //
                    .action(new AppendingComponent<URI>() {
                        @Override
                        protected URI contentFrom(BuildContext out) {
                            return Deployments.base(out.get(UriInfo.class));
                        }
                    }) //
                    .body(hiddenAction("deploy")) //
                    .body(input("checksum").label("Checksum").build()) //
                    .build(), //
            buttonGroup() //
                    .button(button().size(L).style(primary).forForm(MAIN_FORM_ID).body(text("Deploy")).build()) //
                    .build() //
            ).build();

    private static final DeployerPage PAGE = deployerPage() //
            .title(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    DeploymentResource target = out.get(DeploymentResource.class);
                    text(target.isNew() ? "Add Deployment" : target.getName().getValue()).writeInlineTo(out);
                }
            }) //
            .body(link(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    text(Deployments.pathAll(out.get(UriInfo.class))).writeInlineTo(out);
                }
            }).body(text("&lt;")).build()) //
            .body(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    DeploymentResource target = out.get(DeploymentResource.class);
                    if (target.isNew())
                        NEW_DEPLOYMENT_FORM.writeTo(out);
                    else
                        EXISTING_DEPLOYMENT_FORM.writeTo(out);
                }
            }) //
            .build();

    @Override
    protected void prepare(BuildContext buildContext) {
        buildContext.put(Navigation.DEPLOYMENTS);
    }

    @Override
    protected Component component() {
        return PAGE;
    }
}
