type PorterBrandProps = {
  align?: "left" | "center";
};

export function PorterBrand({ align = "left" }: PorterBrandProps) {
  return (
    <div className={`porter-brand porter-brand--${align}`} aria-label="Porter">
      <span className="material-symbols-outlined porter-brand__icon">
        local_shipping
      </span>
      <span className="porter-brand__name">Porter</span>
    </div>
  );
}
