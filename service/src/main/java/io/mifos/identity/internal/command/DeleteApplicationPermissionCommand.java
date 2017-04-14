package io.mifos.identity.internal.command;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class DeleteApplicationPermissionCommand {
  private String applicationIdentifier;
  private String permittableGroupIdentifier;

  public DeleteApplicationPermissionCommand() {
  }

  public DeleteApplicationPermissionCommand(String applicationIdentifier, String permittableGroupIdentifier) {
    this.applicationIdentifier = applicationIdentifier;
    this.permittableGroupIdentifier = permittableGroupIdentifier;
  }

  public String getApplicationIdentifier() {
    return applicationIdentifier;
  }

  public void setApplicationIdentifier(String applicationIdentifier) {
    this.applicationIdentifier = applicationIdentifier;
  }

  public String getPermittableGroupIdentifier() {
    return permittableGroupIdentifier;
  }

  public void setPermittableGroupIdentifier(String permittableGroupIdentifier) {
    this.permittableGroupIdentifier = permittableGroupIdentifier;
  }
}
