import { InteractionType } from '@azure/msal-browser';
import { MsalInterceptorConfiguration } from '@azure/msal-angular';
import { environment } from '../environments/environment';

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>([
    ['https://i978rx7sdd.execute-api.us-east-1.amazonaws.com/*', environment.azure.protectedResourceScopes],
    [`${environment.apiBaseUrl}/*`, environment.azure.protectedResourceScopes],
    ['/api/*', environment.azure.protectedResourceScopes],
    ['http://localhost:8085/*', environment.azure.protectedResourceScopes]
  ]);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}
