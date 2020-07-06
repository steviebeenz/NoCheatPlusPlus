пакет xyz.msw.nope.command;

імпортувати xyz.msws.nope.utils.MSG;

/**
 * Представляє собою результат команди.
 * 
 * @автор імодм
 *
 */
публічний перезапис {
	/**
	 * Команду успішно завершено. Має бути задане повідомлення про успіх
	 * надіслано.
	 */
	УСПІШНО
	/**
	 * Відправник не має потрібних дозволів для команди.
	 */
	Ще одна історія,
	/**
	 * Не вистачає аргументу.
	 */
	Аргумент тощо
	/**
	 * Указано неприпустимий аргумент.
	 */
	Доставка з торгівлею,
	/**
	 * Тільки гравець може використовувати команду і відправник не є.
	 */
	Доступний тільки для гравців,
	/**
	 * Екстрактор не дав гравцю, такий самий як
	 * {@link CommandResult#MISSING_ARGUMENT} але більш конкретні
	 */
	Переважна репутація,
	/**
	 * Сталася невідома помилка
	 */
	ПОМИЛКА;

	публічний String getMessage() {
		перемикач (це) {
			риса ІНШИЙ_ВЕРСІЯ:
				повернення MSG.getString("Command.InvalidArgument", "&cA було надано неприпустимий аргумент.");
			власною функцією:
				повернути MSG.getString("Command.MissingArgument", "&cYou не вистачає аргумента.");
			не_ЗМІЩЕННЯ кейсів:
				повернути MSG.getString("Command.NoPermission",
						"&4&l[&c&lNOPE&4&l] &cУ вас не вистачає &a%perm% &cpermission.");
			справа з ГРАВЦЯ:
				повернути MSG.getString("Command.PlayerOnly",
						"&cВи повинні вказати гравцю як консоль.");
			кейс і є повторною роботою:
				повернути MSG.getString("Command.SpecifyPlayer", "&cYou повинні вказати гравця в якості аргументу.");
			УСПІШНО кейс:
				повернення "";
			за замовчуванням:
				перерва;
		}
		return "&4An сталася помилка при виконанні команди.";
	}
}