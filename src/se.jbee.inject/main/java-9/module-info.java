/*
 *  Copyright (c) 2012-2020, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Defines the API of Silk DI.
 *
 * @uses se.jbee.inject.bind.Bundle
 * @uses se.jbee.inject.bind.ModuleWith
 */
module se.jbee.inject {
  exports se.jbee.inject;
  exports se.jbee.inject.action;
  exports se.jbee.inject.bind;
  exports se.jbee.inject.binder;
  exports se.jbee.inject.bootstrap;
  exports se.jbee.inject.config;
  exports se.jbee.inject.container;
  exports se.jbee.inject.defaults;
  exports se.jbee.inject.event;
  exports se.jbee.inject.scope;

  requires transitive java.logging;
  requires java.desktop;
  requires java.management;

  uses se.jbee.inject.bind.Bundle;
  uses se.jbee.inject.bind.ModuleWith;
}
