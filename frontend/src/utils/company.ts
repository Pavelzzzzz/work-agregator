export function companyScanVariant(status: string): "green" | "yellow" | "red" | "gray" | "blue" {
  switch (status) {
    case "ACTIVE":
      return "green";
    case "PENDING":
      return "yellow";
    case "ERROR":
      return "red";
    default:
      return "gray";
  }
}
