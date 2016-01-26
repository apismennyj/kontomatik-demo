function embedKontomatik(options) {

  var baseUrl = function() {
    return window['internal_kx_service_url'] || "https://signin.kontomatik.com/?";
  };

  var dropPath = function(url) {
    return /^(http[s]{0,1}:\/\/[a-zA-Z0-9.:]+)/.exec(url)[0];
  };

  var parseJson = function(text) {
    try {
      return JSON.parse(text);
    } catch(e) {
      return {};
    }
  };

  var callback = function (event) {
    if (event.origin.indexOf(dropPath(baseUrl())) !== 0)
      return;
    var json = typeof event.data === 'string' ? parseJson(event.data) : event.data;
    if (json.kontomatik && json.kontomatik.error)
      callOnError(json);
    else if (json.kontomatik)
      callOnSuccess(json);
  };

  var callOnError = function(json) {
    if (options.onError)
      options.onError(json.kontomatik.error.exception);
  };

  var callOnSuccess = function(json) {
    options.onSuccess(json.kontomatik.target, json.kontomatik.sessionId, json.kontomatik.sessionIdSignature, json.kontomatik.credentials);
  };

  var url = function () {
    var url = baseUrl();
    var names = ['client', 'locale', 'country', 'ownerExternalId', 'showFavicons'];
    for (var i = 0; i < names.length; i++)
      if (options.hasOwnProperty(names[i]))
        url += names[i] + '=' + options[names[i]] + '&';
    return url.replace(/&$/g, '');
  }();

  if (window.addEventListener)
    window.addEventListener('message', callback, false);
  else
    window.attachEvent('onmessage', callback);

  document.getElementById(options.divId).innerHTML = '<iframe src="' + url + '" style="width: 400px; height: 560px; margin: 0; padding: 0; border: none; overflow: hidden;" scrolling="no"></iframe>';
}

// @Deprecated
function embedKontox(options) {
  embedKontomatik(options);
}
