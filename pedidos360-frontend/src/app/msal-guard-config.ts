import { InteractionType } from '@azure/msal-browser';
import { MsalGuardConfiguration } from '@azure/msal-angular';
import { environment } from '../environments/environment';

export function msalGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: environment.azure.protectedResourceScopes
    }
  };
}

