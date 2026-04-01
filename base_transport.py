"""
base_transport.py - Abstract base class for all transport layers
================================================================

PURPOSE
-------
Defines a common interface that all physical transport layers must implement.
Both the existing RS485Transport and the new BLETransport extend this class,
enabling polymorphic use in higher-level controllers and consistent patterns
across transport backends.

DESIGN PRINCIPLES
-----------------
  - Pure interface: no I/O, no hardware knowledge, no protocol details.
  - Lightweight: only lifecycle methods (connect / disconnect / is_connected).
  - Context-manager support (with statement) for deterministic cleanup.
  - Subclasses own all implementation details; this class owns only the contract.

INHERITANCE TREE
----------------
  BaseTransport (this file)
    |- RS485Transport  (pump_controller.py)   -- existing, not modified
    |- BLETransport    (ble_controller.py)    -- new BLE module
"""

import logging
from abc import ABC, abstractmethod

logger = logging.getLogger(__name__)


class BaseTransport(ABC):
    """Abstract base class defining the lifecycle interface for transport layers.

    Any class that provides physical communication (serial, BLE, TCP, etc.)
    should extend this class and implement the three abstract methods.

    Subclasses may add transport-specific methods (e.g., send_recv for RS485,
    write_gatt for BLE) on top of this shared base.
    """

    def __init__(self) -> None:
        self._connected: bool = False

    # ── Abstract interface (subclasses must override) ─────────────────────

    @abstractmethod
    def connect(self) -> bool:
        """Establish the physical connection.

        Returns:
            True  if the connection was successfully established.
            False if the connection attempt failed (caller may retry).

        Implementations should be idempotent: calling connect() on an already-
        connected transport is safe and should return True without side-effects.
        """
        raise NotImplementedError

    @abstractmethod
    def disconnect(self) -> None:
        """Close the physical connection and release all resources.

        Implementations should be idempotent: calling disconnect() on an
        already-disconnected transport is safe and raises no exception.
        """
        raise NotImplementedError

    @abstractmethod
    def is_connected(self) -> bool:
        """Return True if the transport layer currently has an active connection."""
        raise NotImplementedError

    # ── Context-manager support ───────────────────────────────────────────

    def __enter__(self) -> "BaseTransport":
        self.connect()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> bool:
        self.disconnect()
        return False   # do not suppress exceptions
