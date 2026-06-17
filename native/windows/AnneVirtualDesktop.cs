using System;
using System.Runtime.InteropServices;

namespace AnneDesktopUtils
{
    internal static class Program
    {
        private static readonly Guid ClsidImmersiveShell =
            new Guid("C2F03A33-21F5-47FA-B4BB-156362A2F239");
        private static readonly Guid ClsidVirtualDesktopManagerInternal =
            new Guid("C5E0CDCA-7B6E-41B2-9FC4-D93975CC467B");

        [DllImport("user32.dll")]
        private static extern IntPtr GetForegroundWindow();

        private static int Main(string[] args)
        {
            try
            {
                if (args.Length == 1 && args[0] == "list")
                {
                    Console.WriteLine(GetInternalManager().GetCount());
                    return 0;
                }

                if (args.Length == 1 && args[0] == "probe")
                {
                    GetDesktop(0);
                    IntPtr window = GetForegroundWindow();
                    IApplicationView view;
                    int result = GetApplicationViewCollection().GetViewForHwnd(window, out view);
                    Marshal.ThrowExceptionForHR(result);
                    return 0;
                }

                int desktopIndex;
                if (args.Length != 2 || !int.TryParse(args[1], out desktopIndex))
                {
                    Console.Error.WriteLine(
                        "Usage: AnneVirtualDesktop.exe <list|probe|switch|move> [zero-based-index]");
                    return 2;
                }

                IVirtualDesktop desktop = GetDesktop(desktopIndex);
                if (args[0] == "switch")
                {
                    GetInternalManager().SwitchDesktop(desktop);
                    return 0;
                }

                if (args[0] == "move")
                {
                    IntPtr window = GetForegroundWindow();
                    if (window == IntPtr.Zero)
                    {
                        Console.Error.WriteLine("No foreground window found.");
                        return 3;
                    }

                    IApplicationView view;
                    int result = GetApplicationViewCollection().GetViewForHwnd(window, out view);
                    Marshal.ThrowExceptionForHR(result);
                    GetInternalManager().MoveViewToDesktop(view, desktop);
                    return 0;
                }

                Console.Error.WriteLine("Unknown command: " + args[0]);
                return 2;
            }
            catch (Exception exception)
            {
                Console.Error.WriteLine(exception);
                return 1;
            }
        }

        private static IVirtualDesktop GetDesktop(int index)
        {
            IVirtualDesktopManagerInternal manager = GetInternalManager();
            int count = manager.GetCount();
            if (index < 0 || index >= count)
            {
                throw new ArgumentOutOfRangeException(
                    "index",
                    "Desktop index " + index + " is outside the available range 0-" + (count - 1) + ".");
            }

            IObjectArray desktops;
            manager.GetDesktops(out desktops);
            try
            {
                object desktop;
                Guid desktopInterface = typeof(IVirtualDesktop).GUID;
                desktops.GetAt(index, ref desktopInterface, out desktop);
                return (IVirtualDesktop)desktop;
            }
            finally
            {
                Marshal.ReleaseComObject(desktops);
            }
        }

        private static IVirtualDesktopManagerInternal GetInternalManager()
        {
            IServiceProvider shell = (IServiceProvider)Activator.CreateInstance(
                Type.GetTypeFromCLSID(ClsidImmersiveShell));
            Guid serviceId = ClsidVirtualDesktopManagerInternal;
            Guid interfaceId = typeof(IVirtualDesktopManagerInternal).GUID;
            return (IVirtualDesktopManagerInternal)shell.QueryService(
                ref serviceId,
                ref interfaceId);
        }

        private static IApplicationViewCollection GetApplicationViewCollection()
        {
            IServiceProvider shell = (IServiceProvider)Activator.CreateInstance(
                Type.GetTypeFromCLSID(ClsidImmersiveShell));
            Guid serviceId = typeof(IApplicationViewCollection).GUID;
            Guid interfaceId = serviceId;
            return (IApplicationViewCollection)shell.QueryService(
                ref serviceId,
                ref interfaceId);
        }

    }

    [ComImport]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    [Guid("6D5140C1-7436-11CE-8034-00AA006009FA")]
    internal interface IServiceProvider
    {
        [return: MarshalAs(UnmanagedType.IUnknown)]
        object QueryService(ref Guid service, ref Guid interfaceId);
    }

    [ComImport]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    [Guid("92CA9DCD-5622-4BBA-A805-5E9F541BD8C9")]
    internal interface IObjectArray
    {
        void GetCount(out int count);
        void GetAt(int index, ref Guid interfaceId, [MarshalAs(UnmanagedType.Interface)] out object value);
    }

    [ComImport]
    [InterfaceType(ComInterfaceType.InterfaceIsIInspectable)]
    [Guid("372E1D3B-38D3-42E4-A15B-8AB2B178F513")]
    internal interface IApplicationView
    {
    }

    [ComImport]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    [Guid("1841C6D7-4F9D-42C0-AF41-8747538F10E5")]
    internal interface IApplicationViewCollection
    {
        int GetViews(out IObjectArray array);
        int GetViewsByZOrder(out IObjectArray array);
        int GetViewsByAppUserModelId(string id, out IObjectArray array);
        int GetViewForHwnd(IntPtr window, out IApplicationView view);
    }

    [ComImport]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    [Guid("3F07F4BE-B107-441A-AF0F-39D82529072C")]
    internal interface IVirtualDesktop
    {
        bool IsViewVisible(IApplicationView view);
        Guid GetId();
        [return: MarshalAs(UnmanagedType.HString)]
        string GetName();
        [return: MarshalAs(UnmanagedType.HString)]
        string GetWallpaperPath();
        bool IsRemote();
    }

    [ComImport]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    [Guid("53F5CA0B-158F-4124-900C-057158060B27")]
    internal interface IVirtualDesktopManagerInternal
    {
        int GetCount();
        void MoveViewToDesktop(IApplicationView view, IVirtualDesktop desktop);
        bool CanViewMoveDesktops(IApplicationView view);
        IVirtualDesktop GetCurrentDesktop();
        void GetDesktops(out IObjectArray desktops);
        [PreserveSig]
        int GetAdjacentDesktop(IVirtualDesktop from, int direction, out IVirtualDesktop desktop);
        void SwitchDesktop(IVirtualDesktop desktop);
        void SwitchDesktopAndMoveForegroundView(IVirtualDesktop desktop);
    }

}
