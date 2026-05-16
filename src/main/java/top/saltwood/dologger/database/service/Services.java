package top.saltwood.dologger.database.service;

public class Services {

    private final UserService user;
    private final BlockService block;
    private final ContainerService container;
    private final ItemService item;
    private final SessionService session;
    private final ChatService chat;
    private final CommandService command;
    private final LevelService level;
    private final MaterialService material;
    private final EntityService entity;

    public Services() {
        this.user = new UserService();
        this.level = new LevelService();
        this.material = new MaterialService();
        this.entity = new EntityService();
        this.block = new BlockService(user, level, material, entity);
        this.container = new ContainerService(user, level, material);
        this.item = new ItemService(user, level, material);
        this.session = new SessionService(user, level);
        this.chat = new ChatService(user, level);
        this.command = new CommandService(user, level);
    }

    public UserService user() { return user; }
    public BlockService block() { return block; }
    public ContainerService container() { return container; }
    public ItemService item() { return item; }
    public SessionService session() { return session; }
    public ChatService chat() { return chat; }
    public CommandService command() { return command; }
    public LevelService level() { return level; }
    public MaterialService material() { return material; }
    public EntityService entity() { return entity; }

    public boolean isDatabaseAvailable() {
        return DatabaseStatus.isAvailable();
    }
}
