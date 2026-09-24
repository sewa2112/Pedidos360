import {
  BrowserCacheLocation,
  Configuration,
  LogLevel
} from '@azure/msal-browser';
import { environment } from '../environments/environment';

export const tenantId = environment.azure.tenantId;

export const frontendClientId = environment.azure.clientId;

export const authority = environment.azure.authority;

export const redirectUri = environment.azure.redirectUri;

export const apiScope = environment.azure.protectedResourceScopes[0];

export const msalConfig: Configuration = {
  auth: {
    clientId: frontendClientId,
    authority: authority,
    redirectUri: redirectUri,
    postLogoutRedirectUri: redirectUri
  },
  cache: {
    cacheLocation: BrowserCacheLocation.LocalStorage
  },
  system: {
    allowPlatformBroker: false,
    loggerOptions: {
      loggerCallback: (
        logLevel: LogLevel,
        message: string,
        containsPii: boolean
      ): void => {
        if (containsPii) {
          return;
        }
        console.log(`[MSAL ${LogLevel[logLevel]}] ${message}`);
      },
      logLevel: LogLevel.Info,
      piiLoggingEnabled: false
    }
  }
};

export const loginRequest = {
  scopes: [
    'openid',
    'profile',
    apiScope
  ]
};