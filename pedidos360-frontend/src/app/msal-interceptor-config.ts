import { InteractionType } from '@azure/msal-browser';
import { MsalInterceptorConfiguration } from '@azure/msal-angular';
import { environment } from '../environments/environment';

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>([
    [`${environment.apiBaseUrl}/*`, environment.azure.protectedResourceScopes]
  ]);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}

