namespace Kono.Api.Domain;

public enum VerticalKind
{
    Restaurants = 0,
    Groceries = 1,
    Pharmacy = 2,
    Sweets = 3,
    Flowers = 4,
    Water = 5
}

public enum UserRole
{
    Customer = 0,
    Merchant = 1,
    Courier = 2,
    Admin = 3
}

/// <summary>Single source of truth for an order's lifecycle; the customer,
/// merchant and courier views are all projections of this.</summary>
public enum OrderStage
{
    Placed = 0,
    Confirmed = 1,
    Preparing = 2,
    ReadyForPickup = 3,
    CourierAssigned = 4,
    PickedUp = 5,
    OnTheWay = 6,
    Arrived = 7,
    Delivered = 8,
    Cancelled = 9
}
