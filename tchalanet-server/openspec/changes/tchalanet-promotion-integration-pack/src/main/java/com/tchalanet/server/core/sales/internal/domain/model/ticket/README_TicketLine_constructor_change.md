# TicketLine constructor impact

The sample `PromotionalTicketLineFactory` assumes `TicketLine` is extended with:

```java
Money payoutBaseAmount;
TicketLinePromotionFields promotionFields;
```

If existing constructor cannot be changed immediately, add factory methods:

```java
TicketLine.customerPaid(...)
TicketLine.promotional(...)
TicketLine.withPromotionalPricing(...)
```

Do not create a parallel `PromotionalTicketLine` aggregate. Promotional lines must remain `TicketLine`.
