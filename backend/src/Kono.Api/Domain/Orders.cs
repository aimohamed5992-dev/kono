namespace Kono.Api.Domain;

public class Order : BaseEntity
{
    public string ShortCode { get; set; } = "";      // KON-4821

    public int CustomerUserId { get; set; }
    public User Customer { get; set; } = null!;

    public int StoreId { get; set; }
    public Store Store { get; set; } = null!;

    public int? CourierId { get; set; }
    public Courier? Courier { get; set; }

    public OrderStage Stage { get; set; } = OrderStage.Placed;
    public DateTime PlacedAt { get; set; } = DateTime.UtcNow;
    public DateTime? DeliveredAt { get; set; }
    public bool IsDelivery { get; set; } = true;
    public string? ScheduledForAr { get; set; }
    public string PaymentLabelAr { get; set; } = "Apple Pay";
    public string? NoteAr { get; set; }

    // delivery address snapshot
    public string AddressLabelAr { get; set; } = "";
    public string AddressLineAr { get; set; } = "";
    public double AddressLat { get; set; }
    public double AddressLng { get; set; }

    // money
    public decimal Subtotal { get; set; }
    public decimal DeliveryFee { get; set; }
    public decimal ServiceFee { get; set; }
    public decimal Discount { get; set; }
    public decimal Tip { get; set; }
    public decimal Total { get; set; }
    public decimal CourierPayout { get; set; }

    public List<OrderItem> Items { get; set; } = new();
    public List<OrderEvent> Events { get; set; } = new();

    public int ItemCount => Items.Sum(i => i.Quantity);
}

public class OrderItem : BaseEntity
{
    public int OrderId { get; set; }
    public Order Order { get; set; } = null!;

    public int? ProductId { get; set; }
    public Product? Product { get; set; }

    public string NameAr { get; set; } = "";          // snapshot
    public string ImageUrl { get; set; } = "";
    public decimal UnitPrice { get; set; }
    public int Quantity { get; set; } = 1;
    public string SelectedOptionsAr { get; set; } = ""; // " · " joined
    public string NoteAr { get; set; } = "";
    public decimal LineTotal { get; set; }
}

public class OrderEvent : BaseEntity
{
    public int OrderId { get; set; }
    public Order Order { get; set; } = null!;
    public OrderStage Stage { get; set; }
    public DateTime At { get; set; } = DateTime.UtcNow;
    public string? Note { get; set; }
}
