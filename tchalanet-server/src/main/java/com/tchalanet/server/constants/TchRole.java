package com.tchalanet.server.constants;

public enum TchRole {
  SUPER_ADMIN, // platform-wide
  ADMIN, // tenant admin
  MANAGER, // e.g. supervises cashiers
  CASHIER, // POS operator
  CUSTOMER, // maybe self-service
  OPERATOR // backoffice ops
}
